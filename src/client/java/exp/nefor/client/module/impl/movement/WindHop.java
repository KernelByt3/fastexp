package exp.nefor.client.module.impl.movement;

import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.system.rotation.SmoothRotationManager;
import exp.nefor.client.util.Color;
import exp.nefor.client.util.client.MultiActionsBypass;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class WindHop extends Module {

    private static final float TARGET_PITCH = 89.0f;
    /** Допуск silent-ротации перед броском — Grim прощает небольшие отклонения. */
    private static final float AIM_TOLERANCE = 8.0f;
    private static final long AIM_TIMEOUT_MS = 600;

    private final KeybindSetting hop = new KeybindSetting("Бинд WindHop", this::startHop);

    private boolean active = false;
    private int stage = 0;
    private long timer = 0;
    private int windSlot = -1;
    private int prevSlot = -1;
    public static long lastWindMs = 0;

    public WindHop() {
        super("WindHop", "Кидает заряд ветра под себя (обход MultiActions)", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(hop);
    }

    public boolean isActive() { return active; }

    private void startHop() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || active) return;
        if (!client.player.isOnGround() && client.player.getVelocity().y < -0.3) {
            RenderSystem.notification("Сначала приземлись", Color.RED);
            return;
        }
        if (findWindCharge() == -1) {
            RenderSystem.notification("Нет заряда ветра", Color.RED);
            return;
        }
        // паузим KillAura чтобы не конфликтовать по ротации (BadPacketsJ)
        var ka = exp.nefor.client.module.ModuleManager.get(exp.nefor.client.module.impl.combat.KillAura.class);
        if (ka != null && ka.getTarget() != null) {
            SmoothRotationManager.reset();
            RotationUtil.reset();
        }
        active = true;
        stage = 0;
        timer = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        MultiActionsBypass.tick();
        if (!active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            stop();
            return;
        }
        long now = System.currentTimeMillis();

        switch (stage) {
            // Начинаем плавный доворот вниз. Камеру игрока НЕ трогаем —
            // работает только silent-ротация через RotationUtil (миксин подменяет
            // yaw/pitch только в пакетах, Grim видит легитный плавный поворот).
            case 0 -> {
                player.setSprinting(false);
                client.options.sprintKey.setPressed(false);
                aimDown(player);
                stage = 1;
                timer = now;
            }
            // Ждём пока silent-ротация доплывёт до ~89°. Каждый тик обновляем
            // цель чтобы SmoothRotationManager не протух (таймаут 380мс внутри).
            case 1 -> {
                aimDown(player);
                if (aimReady() || now - timer > AIM_TIMEOUT_MS) {
                    windSlot = findWindChargeHotbar();
                    boolean needSwap = false;
                    if (windSlot == -1) {
                        windSlot = findWindChargeInv();
                        if (windSlot == -1) { stop(); break; }
                        needSwap = true;
                    }
                    if (needSwap) {
                        boolean moving = client.options.forwardKey.isPressed() || client.options.backKey.isPressed()
                                || client.options.leftKey.isPressed() || client.options.rightKey.isPressed();
                        if (moving) {
                            // отпускаем WASD на момент свапа: Grim видит input not moving → не флаг MultiActionsC/Simulation
                            client.options.forwardKey.setPressed(false);
                            client.options.backKey.setPressed(false);
                            client.options.leftKey.setPressed(false);
                            client.options.rightKey.setPressed(false);
                            client.options.sprintKey.setPressed(false);
                            player.setSprinting(false);
                            if (now - timer < AIM_TIMEOUT_MS + 60) break;
                        }
                    }
                    prevSlot = player.getInventory().getSelectedSlot();
                    player.setSprinting(false);
                    if (needSwap) moveToHand(player, windSlot);
                    else player.getInventory().setSelectedSlot(windSlot);
                    stage = 2;
                    timer = now;
                }
            }
            // Держим доворот, затем бросок — СТРОГО по готовности.
            // Бросок мимо доворота = заряд летит не вниз + паливо ротации,
            // поэтому без aimReady только ждём, а по таймауту — отмена без броска.
            // Никаких ручных Look-пакетов — лишний PlayerMoveC2SPacket.LookAndOnGround
            // в том же тике и флагит Grim TickTimer (flying/end) + BadPacketsJ.
            // Silent-ротация через миксин уже подменяет yaw/pitch в обычных пакетах движения.
            case 2 -> {
                aimDown(player);
                if (!aimReady()) {
                    if (now - timer > 1200) {
                        restoreSlot(player);
                        SmoothRotationManager.reset();
                        RotationUtil.reset();
                        stop();
                    }
                    break;
                }
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                stage = 3;
                timer = now;
            }
            case 3 -> {
                if (now - timer < 150) {
                    aimDown(player);
                    break;
                }
                restoreSlot(player);
                lastWindMs = System.currentTimeMillis();
                SmoothRotationManager.reset();
                RotationUtil.reset();
                prevSlot = -1;
                stop();
            }
        }
    }

    /** Плавная цель вниз: yaw сохраняем, pitch → 89. Камера не дёргается. */
    private void aimDown(ClientPlayerEntity player) {
        float yaw = RotationUtil.isRotating ? RotationUtil.targetYaw : player.getYaw();
        SmoothRotationManager.setTargetWithFactor(yaw, TARGET_PITCH, 0.38f);
    }

    private boolean aimReady() {
        return SmoothRotationManager.isActive()
                && Math.abs(MathHelper.wrapDegrees(SmoothRotationManager.getPitch() - TARGET_PITCH)) <= AIM_TOLERANCE;
    }

    private void moveToHand(ClientPlayerEntity player, int invSlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerInteractionManager im = client.interactionManager;
        ScreenHandler sh = player.playerScreenHandler;
        int screenSlot = invSlot < 9 ? 36 + invSlot : invSlot;
        im.clickSlot(sh.syncId, screenSlot, player.getInventory().getSelectedSlot(), SlotActionType.SWAP, player);
    }

    /** Возврат слота после броска/отмены. */
    private void restoreSlot(ClientPlayerEntity player) {
        if (windSlot >= 9) {
            moveToHand(player, windSlot);
        } else if (prevSlot != -1 && prevSlot != windSlot) {
            player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private void stop() {
        active = false;
        stage = 0;
        windSlot = -1;
    }

    private int findWindChargeHotbar() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return -1;
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        return -1;
    }

    private int findWindChargeInv() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return -1;
        for (int i = 9; i < 36; i++) if (p.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        return -1;
    }

    private int findWindCharge() {
        int h = findWindChargeHotbar();
        if (h != -1) return h;
        return findWindChargeInv();
    }
}
