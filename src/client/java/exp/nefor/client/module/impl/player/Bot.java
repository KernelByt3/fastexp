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
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Bot — категория NPC-двойников. Сколько угодно ботов с никами:
 * Follow/Stay, «Ко мне», вселение (камера + WASD за бота, тело стоит).
 * Окно управления — по бинду, ПКМ по боту в окне = вселение/возврат.
 */
public class Bot extends Module {

    public static final class Entry {
        public final OtherClientPlayerEntity entity;
        public final String nick;
        public boolean follow = true;

        Entry(OtherClientPlayerEntity entity, String nick) {
            this.entity = entity;
            this.nick = nick;
        }
    }

    private final ChoiceSetting mode = new ChoiceSetting("Режим новых", List.of("Follow", "Stay"), 0);
    private final KeybindSetting windowKey = new KeybindSetting("Окно ботов", this::openWindow);
    private final KeybindSetting possessKey = new KeybindSetting("Вселение", this::possessSelected);

    private final List<Entry> bots = new ArrayList<>();
    private Entry selected;
    private Entry possessed;
    private Input savedInput;
    private float parkYaw, parkPitch;
    private int num = 0;

    public Bot() {
        super("Bot", "NPC-двойники: ники, окно, вселение", Category.BOT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(mode, windowKey, possessKey);
    }

    public static Bot get() {
        return exp.nefor.client.module.ModuleManager.get(Bot.class);
    }

    public static boolean isBot(Entity e) {
        Bot b = null;
        try { b = get(); } catch (Exception ignored) {}
        if (b == null || e == null || e.getType() != EntityType.PLAYER) return false;
        for (Entry en : b.bots) if (en.entity.getId() == e.getId()) return true;
        return false;
    }

    public List<Entry> getBots() { return Collections.unmodifiableList(bots); }
    public Entry getSelected() { return selected; }
    public void setSelected(Entry e) { selected = e; }
    public Entry getPossessed() { return possessed; }
    public boolean isPossessing() { return possessed != null; }

    public static String currentServer() {
        try {
            var mc = MinecraftClient.getInstance();
            if (mc.isInSingleplayer()) return "singleplayer";
            var entry = mc.getCurrentServerEntry();
            if (entry != null && entry.address != null) return entry.address;
            var h = mc.getNetworkHandler();
            if (h != null && h.getConnection() != null) return h.getConnection().getAddress().toString();
        } catch (Exception ignored) {}
        return "unknown";
    }

    private void possessSelected() {
        possess(selected);
    }

    private void openWindow() {
        var mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.setScreen(new exp.nefor.client.gui.BotScreen());
    }

    @Override
    public void onDisable() {
        release();
        despawnAll();
    }

    public Entry spawnBot(String nick) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;
        if (nick == null || nick.isBlank()) nick = "Bot-" + (++num);
        nick = nick.trim();
        if (nick.length() > 16) nick = nick.substring(0, 16);
        OtherClientPlayerEntity e = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), nick));
        Vec3d p = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        e.setPosition(p.x + 1.0 + bots.size() * 0.7, p.y, p.z);
        e.setYaw(mc.player.getYaw());
        e.setBodyYaw(mc.player.getYaw());
        e.setCustomName(Text.literal(nick));
        e.setCustomNameVisible(true);
        mc.world.addEntity(e);
        Entry en = new Entry(e, nick);
        en.follow = mode.getValue().equals("Follow");
        bots.add(en);
        if (selected == null) selected = en;
        RenderSystem.notification("Bot " + nick + " добавлен", Color.GREEN);
        return en;
    }

    public void removeBot(Entry en) {
        var mc = MinecraftClient.getInstance();
        if (en == null) return;
        if (possessed == en) release();
        if (mc.world != null) mc.world.removeEntity(en.entity.getId(), Entity.RemovalReason.DISCARDED);
        bots.remove(en);
        if (selected == en) selected = bots.isEmpty() ? null : bots.get(0);
    }

    private void despawnAll() {
        var mc = MinecraftClient.getInstance();
        for (Entry en : new ArrayList<>(bots)) {
            if (mc.world != null) {
                try { mc.world.removeEntity(en.entity.getId(), Entity.RemovalReason.DISCARDED); } catch (Exception ignored) {}
            }
        }
        bots.clear();
        selected = null;
    }

    /** Бот идёт/тп к основе. */
    public void bringToMe(Entry en) {
        var mc = MinecraftClient.getInstance();
        if (en == null || mc.player == null) return;
        Vec3d p = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        en.entity.setPosition(p.x + 1.0, p.y, p.z);
        en.entity.setVelocity(0, 0, 0);
    }

    public void possess(Entry en) {
        var mc = MinecraftClient.getInstance();
        if (en == null || mc.player == null) return;
        if (possessed == en) { release(); return; }
        if (possessed != null) release();
        possessed = en;
        selected = en;
        savedInput = mc.player.input;
        Input dummy = new Input();
        dummy.playerInput = PlayerInput.DEFAULT;
        mc.player.input = dummy;
        parkYaw = mc.player.getYaw();
        parkPitch = mc.player.getPitch();
        mc.setCameraEntity(en.entity);
        RenderSystem.notification("Bot " + en.nick + ": управляешь", Color.GREEN);
    }

    public void release() {
        var mc = MinecraftClient.getInstance();
        if (possessed != null) {
            possessed.entity.setVelocity(0, Math.min(0, possessed.entity.getVelocity().y), 0);
            possessed = null;
        }
        if (mc != null && mc.player != null && savedInput != null) mc.player.input = savedInput;
        savedInput = null;
        if (mc != null && mc.player != null) mc.setCameraEntity(mc.player);
    }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || mc.world == null) {
            if (possessed != null) release();
            return;
        }
        for (Entry en : new ArrayList<>(bots)) {
            if (en.entity.isRemoved()) continue;
            if (en == possessed) {
                possessTick(mc, en);
            } else if (en.follow) {
                followTick(mc, en);
            } else {
                stayTick(en);
            }
        }
    }

    private void stayTick(Entry en) {
        var e = en.entity;
        e.setVelocity(e.getVelocity().x * 0.5, e.getVelocity().y - 0.08, e.getVelocity().z * 0.5);
        e.move(MovementType.SELF, e.getVelocity());
    }

    private void followTick(MinecraftClient mc, Entry en) {
        var e = en.entity;
        double dist = e.distanceTo(mc.player);
        if (dist > 2.5) {
            float yaw = yawTo(new Vec3d(e.getX(), e.getY(), e.getZ()), new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            e.setYaw(yaw);
            e.setBodyYaw(yaw);
            double speed = dist > 8 ? 0.30 : 0.22;
            double rad = Math.toRadians(yaw);
            physicsTick(e, -Math.sin(rad) * speed, Math.cos(rad) * speed, e.horizontalCollision && e.isOnGround());
        } else {
            e.setVelocity(e.getVelocity().x * 0.5, e.getVelocity().y - 0.08, e.getVelocity().z * 0.5);
            e.move(MovementType.SELF, e.getVelocity());
        }
        lookAt(e, mc.player.getEyePos());
    }

    private void possessTick(MinecraftClient mc, Entry en) {
        var e = en.entity;
        e.setYaw(mc.player.getYaw());
        e.setPitch(mc.player.getPitch());
        e.setHeadYaw(mc.player.getYaw());
        e.setBodyYaw(mc.player.getYaw());
        mc.player.setYaw(parkYaw);
        mc.player.setPitch(parkPitch);

        var o = mc.options;
        float f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        float s = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
        float speed = o.sneakKey.isPressed() ? 0.12f : (o.sprintKey.isPressed() ? 0.34f : 0.22f);
        double rad = Math.toRadians(e.getYaw());
        double wx = -Math.sin(rad) * f - Math.cos(rad) * s;
        double wz = Math.cos(rad) * f - Math.sin(rad) * s;
        double len = Math.hypot(wx, wz);
        if (len > 0.001) { wx = wx / len * speed; wz = wz / len * speed; }
        else { wx = 0; wz = 0; }
        physicsTick(e, wx, wz, o.jumpKey.isPressed());
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

    public LivingEntity getBot() { return possessed != null ? possessed.entity : (selected != null ? selected.entity : null); }
}
