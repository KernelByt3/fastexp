package exp.nefor.client.module.impl.movement;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.render.RenderSystem;
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
import org.lwjgl.glfw.GLFW;

public class WindHop extends Module {

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

    public boolean isActive(){ return active; }

    private void startHop() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || active) return;
        if (client.player.isOnGround() == false && client.player.getVelocity().y < -0.3) {
            RenderSystem.notification("Сначала приземлись", Color.RED);
            return;
        }
        if (findWindCharge() == -1) {
            RenderSystem.notification("Нет заряда ветра", Color.RED);
            return;
        }
        // паузим KillAura чтобы не конфликтовать по ротации (BadPacketsJ)
        var ka = exp.nefor.client.module.ModuleManager.get(exp.nefor.client.module.impl.combat.KillAura.class);
        if(ka!=null && ka.getTarget()!=null){
            exp.nefor.client.system.rotation.SmoothRotationManager.reset();
            exp.nefor.client.util.client.RotationUtil.reset();
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
            case 0 -> {
                player.setSprinting(false);
                client.options.sprintKey.setPressed(false);
                float curYaw = player.getYaw();
                float curPitch = player.getPitch();
                float targetPitch = 89.0f;
                float smoothPitch = exp.nefor.client.util.Mathematics.turnSmooth(curPitch, targetPitch, 6f);
                exp.nefor.client.util.client.RotationUtil.setRotationRaw(curYaw, smoothPitch, false);
                exp.nefor.client.system.rotation.SmoothRotationManager.setTarget(curYaw, smoothPitch, exp.nefor.client.system.rotation.RotationProfile.VANILLA);
                stage = 1;
                timer = now;
            }
            case 1 -> {
                if (now - timer < 80) {
                    float curYaw = player.getYaw();
                    float curPitch = player.getPitch();
                    float smoothPitch = exp.nefor.client.util.Mathematics.turnSmooth(curPitch, 89.0f, 6f);
                    var prof = exp.nefor.client.system.rotation.RotationProfile.VANILLA;
                    exp.nefor.client.system.rotation.SmoothRotationManager.setTarget(curYaw, smoothPitch, prof);
                    exp.nefor.client.util.client.RotationUtil.setRotationRaw(curYaw, smoothPitch, false);
                    break;
                }
                windSlot = findWindChargeHotbar();
                boolean needSwap = false;
                if (windSlot == -1) {
                    windSlot = findWindChargeInv();
                    if (windSlot == -1) { stop(); break; }
                    needSwap = true;
                }
                if (needSwap) {
                    boolean moving = client.options.forwardKey.isPressed() || client.options.backKey.isPressed() || client.options.leftKey.isPressed() || client.options.rightKey.isPressed();
                    if (moving) {
                        // замедляем как просишь — отпускаем WASD на момент свапа, Grim видит input not moving → не флаг MultiActionsC/Simulation
                        client.options.forwardKey.setPressed(false);
                        client.options.backKey.setPressed(false);
                        client.options.leftKey.setPressed(false);
                        client.options.rightKey.setPressed(false);
                        client.options.sprintKey.setPressed(false);
                        player.setSprinting(false);
                        if (now - timer < 140) {
                            exp.nefor.client.util.client.RotationUtil.setRotationRaw(player.getYaw(), 89.0f, false);
                            break;
                        }
                    }
                }
                prevSlot = player.getInventory().getSelectedSlot();
                player.setSprinting(false);
                if (needSwap) moveToHand(player, windSlot);
                else client.player.getInventory().setSelectedSlot(windSlot);
                stage = 2;
                timer = now;
            }
            case 2 -> {
                if (now - timer < 90) {
                    float curPitch = player.getPitch();
                    float smoothPitch = exp.nefor.client.util.Mathematics.turnSmooth(curPitch, 89.0f, 6f);
                    exp.nefor.client.system.rotation.SmoothRotationManager.setTarget(player.getYaw(), smoothPitch, exp.nefor.client.system.rotation.RotationProfile.VANILLA);
                    exp.nefor.client.util.client.RotationUtil.setRotationRaw(player.getYaw(), smoothPitch, false);
                    break;
                }
                float ry = player.getYaw();
                float rp = player.getPitch();
                float useYaw = player.getYaw();
                float usePitch = 89.0f;
                // если уже смотришь вниз — не крутим, иначе AimModulo360
                if(Math.abs(player.getPitch() - 89f) > 35){
                    // плавно только pitch, yaw не трогаем
                    player.setPitch(usePitch);
                    exp.nefor.client.util.client.RotationUtil.setRotationRaw(useYaw, usePitch, false);
                    // форсим тик-пакет с 89 чтобы BadPacketsJ совпал
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(useYaw, usePitch, player.isOnGround(), player.horizontalCollision));
                    // ждём ещё тик
                    if(now - timer < 130){
                        exp.nefor.client.util.client.RotationUtil.setRotationRaw(useYaw, usePitch, false);
                        break;
                    }
                }
                player.setYaw(useYaw);
                player.setPitch(usePitch);
                exp.nefor.client.util.client.RotationUtil.setRotationRaw(useYaw, usePitch, false);
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                player.setYaw(ry);
                player.setPitch(rp);
                stage = 3;
                timer = now;
            }
            case 3 -> {
                if (now - timer < 140) break;
                if (windSlot >= 9) {
                    if (player.getVelocity().horizontalLength() > 0.08) {
                        timer = now - 100;
                        break;
                    }
                    moveToHand(player, windSlot);
                } else if (prevSlot != -1 && prevSlot != windSlot) {
                    client.player.getInventory().setSelectedSlot(prevSlot);
                }
                lastWindMs = System.currentTimeMillis();
                exp.nefor.client.system.rotation.SmoothRotationManager.reset();
                RotationUtil.reset();
                prevSlot = -1;
                stop();
            }
        }
    }

    private void moveToHand(ClientPlayerEntity player, int invSlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerInteractionManager im = client.interactionManager;
        ScreenHandler sh = player.playerScreenHandler;
        int screenSlot = invSlot < 9 ? 36 + invSlot : invSlot;
        im.clickSlot(sh.syncId, screenSlot, player.getInventory().getSelectedSlot(), SlotActionType.SWAP, player);
    }

    private void stop() {
        active = false;
        stage = 0;
        windSlot = -1;
    }

    private int findWindChargeHotbar() {
        var p = MinecraftClient.getInstance().player;
        if (p==null) return -1;
        for (int i=0;i<9;i++) if(p.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        return -1;
    }
    private int findWindChargeInv() {
        var p = MinecraftClient.getInstance().player;
        if (p==null) return -1;
        for (int i=9;i<36;i++) if(p.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        return -1;
    }
    private int findWindCharge() {
        int h=findWindChargeHotbar();
        if(h!=-1) return h;
        return findWindChargeInv();
    }
}
