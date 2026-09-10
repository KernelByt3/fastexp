package exp.nefor.client.util.client;

import net.minecraft.client.MinecraftClient;







public final class MultiActionsBypass {

    private static boolean active = false;
    private static long endTime = 0;

    private MultiActionsBypass() {
    }

    public static void start(long durationMs) {
        active = true;
        endTime = System.currentTimeMillis() + durationMs;
    }

    public static void tick() {
        if (active && System.currentTimeMillis() > endTime) active = false;
    }

    public static boolean isActive() {
        return active;
    }
}
