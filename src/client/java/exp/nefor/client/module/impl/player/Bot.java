package exp.nefor.client.module.impl.player;

import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.system.ghost.GhostConnection;
import exp.nefor.client.util.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;







public class Bot extends Module {

    public static final class Entry {
        public final GhostConnection conn;
        public boolean follow = true;

        Entry(GhostConnection conn) {
            this.conn = conn;
        }

        public String nick() { return conn.nick(); }
        public String status() { return conn.status; }
    }

    private final BooleanSetting followNew = new BooleanSetting("Новые идут", true);
    private final KeybindSetting windowKey = new KeybindSetting("Окно ботов", this::openWindow);
    private final KeybindSetting possessKey = new KeybindSetting("Вселение", this::possessSelected);

    private final List<Entry> bots = new ArrayList<>();
    private Entry selected;
    private Entry possessed;
    private Input savedInput;
    private float parkYaw, parkPitch;
    private int num = 0;

    public Bot() {
        super("Bot", "Гости на сервере: ники, окно, вселение", Category.BOT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(followNew, windowKey, possessKey);
    }

    public static Bot get() {
        return exp.nefor.client.module.ModuleManager.get(Bot.class);
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
        } catch (Exception ignored) {}
        return "unknown";
    }

    public static String[] splitServer(String in) {
        String s = in == null ? "" : in.trim();
        int i = s.lastIndexOf(':');
        if (i > 0 && i < s.length() - 1) {
            try {
                return new String[]{s.substring(0, i), String.valueOf(Integer.parseInt(s.substring(s.lastIndexOf(':') + 1)))};
            } catch (Exception ignored) {
            }
        }
        return new String[]{s, "25565"};
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
        for (Entry en : new ArrayList<>(bots)) removeBot(en);
    }

    public Entry spawnBot(String nick, String server) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;
        if (mc.isInSingleplayer()) {
            RenderSystem.notification("Боты только на сервере", Color.RED);
            return null;
        }
        // без включённого модуля тик не качает коннект — включаем сами
        if (!isEnabled()) setEnabled(true);
        if (nick == null || nick.isBlank()) nick = "Bot-" + (++num);
        nick = nick.trim();
        if (nick.length() > 16) nick = nick.substring(0, 16);
        String[] hp = splitServer(server == null || server.isBlank() ? currentServer() : server);
        int port;
        try {
            port = Integer.parseInt(hp[1]);
        } catch (Exception e) {
            port = 25565;
        }
        GhostConnection conn = new GhostConnection(
                exp.nefor.client.system.ghost.GhostSession.of(nick), hp[0], port);
        Entry en = new Entry(conn);
        en.follow = followNew.getValue();
        bots.add(en);
        if (selected == null) selected = en;
        conn.connect();
        RenderSystem.notification("Bot " + nick + " коннектится", Color.GREEN);
        return en;
    }

    public void removeBot(Entry en) {
        if (en == null) return;
        if (possessed == en) release();
        try {
            en.conn.disconnect();
        } catch (Exception ignored) {
        }
        bots.remove(en);
        if (selected == en) selected = bots.isEmpty() ? null : bots.get(0);
    }

    
    public void bringToMe(Entry en) {
        var mc = MinecraftClient.getInstance();
        if (en == null || mc.player == null) return;
        en.conn.setTarget(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        en.follow = true;
    }

    public void possess(Entry en) {
        var mc = MinecraftClient.getInstance();
        if (en == null || mc.player == null || !en.conn.playReady) return;
        if (possessed == en) {
            release();
            return;
        }
        if (possessed != null) release();
        possessed = en;
        selected = en;
        savedInput = mc.player.input;
        Input dummy = new Input();
        dummy.playerInput = PlayerInput.DEFAULT;
        mc.player.input = dummy;
        parkYaw = mc.player.getYaw();
        parkPitch = mc.player.getPitch();
        RenderSystem.notification("Bot " + en.nick() + ": WASD вслепую", Color.GREEN);
    }

    public void release() {
        var mc = MinecraftClient.getInstance();
        possessed = null;
        if (mc != null && mc.player != null && savedInput != null) mc.player.input = savedInput;
        savedInput = null;
    }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || mc.world == null) {
            if (possessed != null) release();
            return;
        }
        for (Entry en : new ArrayList<>(bots)) {
            GhostConnection c = en.conn;
            if (c.dead && c != null && possessed == en) release();
            if (en == possessed) {
                drivePossessed(mc, c);
            } else if (en.follow && c.playReady) {
                double d = Math.hypot(c.player.x - mc.player.getX(), c.player.z - mc.player.getZ());
                if (d > 3.0) c.setTarget(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                else c.clearTarget();
            }
            c.tick();
        }
    }

    private void drivePossessed(MinecraftClient mc, GhostConnection c) {
        mc.player.setYaw(parkYaw);
        mc.player.setPitch(parkPitch);
        var o = mc.options;
        float f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        float s = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
        if (Math.abs(f) < 0.01 && Math.abs(s) < 0.01) {
            c.clearTarget();
            return;
        }
        double rad = Math.toRadians(mc.player.getYaw());
        double wx = -Math.sin(rad) * f - Math.cos(rad) * s;
        double wz = Math.cos(rad) * f - Math.sin(rad) * s;
        double len = Math.hypot(wx, wz);
        wx = wx / len * 3.0;
        wz = wz / len * 3.0;
        c.setTarget(c.player.x + wx, c.player.y, c.player.z + wz);
    }
}
