package exp.nefor.client.module.impl.player;

import com.mojang.authlib.GameProfile;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.setting.ChoiceSetting;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

/**
 * Bot — NPC-двойник. Follow/Stay, а по бинду можно ВСЕЛИТЬСЯ:
 * камера и WASD управляют ботом, основное тело стоит на месте.
 * Повторный бинд — возврат в основное тело.
 */
public class Bot extends Module {

    private final ChoiceSetting mode = new ChoiceSetting("Режим", List.of("Follow", "Stay"), 0);
    private final KeybindSetting possessKey = new KeybindSetting("Вселение", this::togglePossess);

    private OtherClientPlayerEntity bot;
    private static int botId = -1;

    private boolean possessing = false;
    private Input savedInput;
    private float parkYaw, parkPitch;

    public Bot() {
        super("Bot", "NPC-двойник: ходит за тобой, можно вселиться", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(mode, possessKey);
    }

    public static boolean isBot(Entity e) {
        return e != null && e.getId() == botId && e.getType() == EntityType.PLAYER;
    }

    @Override
    public void onEnable() {
        spawn();
    }

    @Override
    public void onDisable() {
        if (possessing) release();
        despawn();
    }

    private void spawn() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        despawn();
        bot = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "Bot"));
        Vec3d p = mc.player.getPos();
        bot.setPosition(p.x + 1.0, p.y, p.z);
        bot.setYaw(mc.player.getYaw());
        bot.setBodyYaw(mc.player.getYaw());
        bot.setCustomName(Text.literal("Bot"));
        bot.setCustomNameVisible(true);
        mc.world.addEntity(bot);
        botId = bot.getId();
        RenderSystem.notification("Bot заспавнен — бинд вселения в настройках", Color.GREEN);
    }

    private void despawn() {
        var mc = MinecraftClient.getInstance();
        if (bot != null && mc.world != null) {
            mc.world.removeEntity(bot.getId(), Entity.RemovalReason.DISCARDED);
        }
        bot = null;
        botId = -1;
    }

    private void togglePossess() {
        var mc = MinecraftClient.getInstance();
        if (bot == null || mc.player == null) return;
        if (possessing) {
            release();
            RenderSystem.notification("Bot: возврат в тело", Color.GREEN);
        } else {
            possessing = true;
            savedInput = mc.player.input;
            Input dummy = new Input();
            dummy.playerInput = PlayerInput.DEFAULT;
            mc.player.input = dummy;
            parkYaw = mc.player.getYaw();
            parkPitch = mc.player.getPitch();
            mc.setCameraEntity(bot);
            RenderSystem.notification("Bot: управляешь ботом", Color.GREEN);
        }
    }

    private void release() {
        var mc = MinecraftClient.getInstance();
        possessing = false;
        if (mc.player != null && savedInput != null) mc.player.input = savedInput;
        savedInput = null;
        if (bot != null) bot.setVelocity(0, Math.min(0, bot.getVelocity().y), 0);
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || mc.world == null) {
            if (possessing) release();
            if (bot != null && mc.world == null) { bot = null; botId = -1; }
            return;
        }
        if (bot == null) return;

        if (possessing) {
            possessTick(mc);
            return;
        }

        if (mode.getValue().equals("Stay")) {
            bot.setVelocity(bot.getVelocity().x * 0.5, bot.getVelocity().y - 0.08, bot.getVelocity().z * 0.5);
            bot.move(MovementType.SELF, bot.getVelocity());
            lookAt(bot, mc.player.getEyePos());
            return;
        }

        // Follow
        double dist = bot.distanceTo(mc.player);
        if (dist > 2.5) {
            float yaw = yawTo(bot.getPos(), mc.player.getPos());
            bot.setYaw(yaw);
            bot.setBodyYaw(yaw);
            double speed = dist > 8 ? 0.30 : 0.22;
            double rad = Math.toRadians(yaw);
            boolean jump = bot.horizontalCollision && bot.isOnGround();
            physicsTick(bot, -Math.sin(rad) * speed, Math.cos(rad) * speed, jump);
        } else {
            bot.setVelocity(bot.getVelocity().x * 0.5, bot.getVelocity().y - 0.08, bot.getVelocity().z * 0.5);
            bot.move(MovementType.SELF, bot.getVelocity());
        }
        lookAt(bot, mc.player.getEyePos());
    }

    private void possessTick(MinecraftClient mc) {
        // взгляд мыши — боту, тело остаётся запаркованным
        bot.setYaw(mc.player.getYaw());
        bot.setPitch(mc.player.getPitch());
        bot.setHeadYaw(mc.player.getYaw());
        bot.setBodyYaw(mc.player.getYaw());
        mc.player.setYaw(parkYaw);
        mc.player.setPitch(parkPitch);

        var o = mc.options;
        float f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        float s = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
        float speed = o.sneakKey.isPressed() ? 0.12f : (o.sprintKey.isPressed() ? 0.34f : 0.22f);
        double rad = Math.toRadians(bot.getYaw());
        double wx = (-Math.sin(rad) * f - Math.cos(rad) * s);
        double wz = (Math.cos(rad) * f - Math.sin(rad) * s);
        double len = Math.hypot(wx, wz);
        if (len > 0.001) { wx = wx / len * speed; wz = wz / len * speed; }
        else { wx = 0; wz = 0; }
        physicsTick(bot, wx, wz, o.jumpKey.isPressed());
    }

    private static void physicsTick(OtherClientPlayerEntity e, double wx, double wz, boolean jump) {
        double vy = e.getVelocity().y - 0.08;
        if (vy < -3) vy = -3;
        if (e.isTouchingWater()) vy = Math.min(vy + 0.06, 0.1);
        if (e.isOnGround() && jump) vy = 0.42;
        e.setVelocity(wx, vy, wz);
        e.move(MovementType.SELF, e.getVelocity());
    }

    private static float yawTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        return MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
    }

    private static void lookAt(OtherClientPlayerEntity e, Vec3d target) {
        Vec3d eye = e.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = MathHelper.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))), -90f, 90f);
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setHeadYaw(yaw);
        e.setBodyYaw(yaw);
    }

    public LivingEntity getBot() { return bot; }
    public boolean isPossessing() { return possessing; }
}
