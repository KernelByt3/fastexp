package exp.nefor.client.util.client;

import net.minecraft.client.MinecraftClient;

/**
 * Bypass for server-side "MultiActions" checks: while a quick item interaction
 * (e.g. wind-charge throw) happens, the player must not be moving fast.
 * Call {@link #start(long)} right before the action and the player's horizontal
 * velocity is zeroed for the given duration, then it auto-disables.
 */
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
