package exp.nefor.client.system.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * GCD (mouse granularity) — отдельный файл как просили.
 * Minecraft двигает камеру только кратно GCD, иначе палится.
 */
public final class GcdUtil {
    private GcdUtil() {}

    public static float getGcd() {
        var mc = MinecraftClient.getInstance();
        double sens = 0.5;
        try { sens = mc.options.getMouseSensitivity().getValue(); } catch (Exception ignored) {}
        double f = sens * 0.6 + 0.2;
        return (float)(f * f * f * 8.0 * 0.15);
    }

    /** Снаппит дельту к сетке GCD. */
    public static float snap(float delta, float gcd) {
        if (gcd < 0.0001f) return delta;
        return (float)(Math.round(delta / gcd) * gcd);
    }

    /** Снаппит целевой угол относительно базы. */
    public static float snapAngle(float base, float target, float gcd) {
        float delta = MathHelper.wrapDegrees(target - base);
        return MathHelper.wrapDegrees(base + snap(delta, gcd));
    }

    /** Проверка что угол уже на сетке GCD (допуск). */
    public static boolean isSnapped(float angle, float gcd) {
        if (gcd < 0.0001f) return true;
        float r = angle % gcd;
        return Math.abs(r) < 0.001f || Math.abs(r - gcd) < 0.001f;
    }
}
