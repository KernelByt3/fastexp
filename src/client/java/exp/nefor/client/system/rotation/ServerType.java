package exp.nefor.client.system.rotation;

import net.minecraft.client.MinecraftClient;

public enum ServerType {
    VANILLA("Vanilla"),
    HYPIXEL("Hypixel"),
    REALLY_WORLD("ReallyWorld"),
    FUN_TIME("FunTime"),
    HOLY_WORLD("HolyWorld"),
    SPOOKY_TIME("SpookyTime");

    private final String displayName;
    ServerType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }

    public static ServerType detect() {
        try {
            var mc = MinecraftClient.getInstance();
            if (mc.getCurrentServerEntry() == null || mc.getCurrentServerEntry().address == null) return VANILLA;
            String ip = mc.getCurrentServerEntry().address.toLowerCase();
            if (ip.contains("hypixel")) return HYPIXEL;
            if (ip.contains("reallyworld") || ip.contains("play.reallyworld.ru") || ip.contains("rw.")) return REALLY_WORLD;
            if (ip.contains("funtime") || ip.contains("play.funtime.su")) return FUN_TIME;
            if (ip.contains("holyworld") || ip.contains("play.holyworld.ru") || ip.contains("hw.")) return HOLY_WORLD;
            if (ip.contains("spooky") || ip.contains("spookytime.net") || ip.contains("play.spookytime")) return SPOOKY_TIME;
        } catch (Exception ignored) {}
        return VANILLA;
    }
}
