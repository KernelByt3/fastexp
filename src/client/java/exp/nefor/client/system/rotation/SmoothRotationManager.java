package exp.nefor.client.system.rotation;

import exp.nefor.client.util.Mathematics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Плавный менеджер ротаций — отдельный файл.
 * Убирает дергание: интерполяция + GCD + clamp скорости.
 * Вызывать каждый тик: SmoothRotationManager.tick()
 * Устанавливать цель: smoothlyRotateTo(yaw, pitch, profile)
 */
public final class SmoothRotationManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static float currentYaw;
    private static float currentPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static boolean active = false;
    private static long lastUpdate = 0;
    private static RotationProfile profile = RotationProfile.VANILLA;

    // для сглаживания
    private static float velocityYaw = 0;
    private static float velocityPitch = 0;

    private SmoothRotationManager() {}

    public static void setTarget(float yaw, float pitch, RotationProfile p) {
        profile = p;
        float wrappedTarget = MathHelper.wrapDegrees(yaw);
        if (!active) {
            currentYaw = mc.player != null ? mc.player.getYaw() : wrappedTarget;
            currentPitch = mc.player != null ? mc.player.getPitch() : pitch;
            active = true;
        }
        float delta = MathHelper.wrapDegrees(wrappedTarget - MathHelper.wrapDegrees(currentYaw));
        targetYaw = currentYaw + delta;
        targetPitch = MathHelper.clamp(pitch, -90f, 90f);
        lastUpdate = System.currentTimeMillis();
    }
    public static void setTargetWithFactor(float yaw, float pitch, float factor){
        // кастомный фактор из нейро-модели — без привязки к профилю
        float wrappedTarget = MathHelper.wrapDegrees(yaw);
        if (!active) {
            currentYaw = mc.player != null ? mc.player.getYaw() : wrappedTarget;
            currentPitch = mc.player != null ? mc.player.getPitch() : pitch;
            active = true;
        }
        float delta = MathHelper.wrapDegrees(wrappedTarget - MathHelper.wrapDegrees(currentYaw));
        targetYaw = currentYaw + delta;
        targetPitch = MathHelper.clamp(pitch, -90f, 90f);
        // временно подменяем smoothing
        float old = profile.smoothing;
        profile = RotationProfile.VANILLA; // dummy
        // хак: храним кастомный фактор в bias через переопределение tick factor
        customFactor = factor;
        lastUpdate = System.currentTimeMillis();
    }
    private static float customFactor = -1f;

    /** Мгновенно — только для тестов, WindHop теперь не использует */
    public static void setInstant(float yaw, float pitch, RotationProfile p) {
        profile = p;
        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90f, 90f);
        float gcd = p.gcdSnap ? GcdUtil.getGcd() : 0f;
        if (gcd > 0.0001f && mc.player != null) {
            yaw = GcdUtil.snapAngle(currentYaw, yaw, gcd);
            pitch = GcdUtil.snapAngle(mc.player.getPitch(), pitch, gcd);
        }
        currentYaw = targetYaw = yaw;
        currentPitch = targetPitch = pitch;
        active = true;
        lastUpdate = System.currentTimeMillis();
        applyToPlayer(currentYaw, currentPitch);
    }

    public static void reset() {
        active = false;
        velocityYaw = velocityPitch = 0;
        lastUpdate = 0;
        customFactor = -1f;
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        exp.nefor.client.util.client.RotationUtil.reset();
    }

    public static boolean isActive() { return active && System.currentTimeMillis() - lastUpdate < 700; }

    public static float getYaw() { return active ? currentYaw : (mc.player != null ? mc.player.getYaw() : 0); }
    public static float getPitch() { return active ? currentPitch : (mc.player != null ? mc.player.getPitch() : 0); }

    private static long lastTickNs = System.nanoTime();
    /** FPS-плавный тик — как рукой 144fps, не робот 20 TPS */
    public static void tick() {
        if (mc.player == null) { reset(); return; }
        if (!active) return;
        if (System.currentTimeMillis() - lastUpdate > 380) { reset(); return; }

        long nowNs = System.nanoTime();
        float dt = (nowNs - lastTickNs) / 1_000_000_000f;
        lastTickNs = nowNs;
        dt = MathHelper.clamp(dt, 0.005f, 0.05f); // 5-50ms
        float fpsFactor = dt * 20f; // нормируем к 20 TPS

        float gcd = profile.gcdSnap ? GcdUtil.getGcd() : 0f;
        // плавный отворот — свой Mathematics чтобы не AimModulo360
        float rawDeltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaYaw = Mathematics.wrapDegrees(Mathematics.turnSmooth(currentYaw, targetYaw, 28f) - currentYaw);
        // если большой отворот, используем turnSmooth delta, иначе raw
        if(Math.abs(rawDeltaYaw) > 90f) deltaYaw = Mathematics.wrapDegrees(Mathematics.turnSmooth(currentYaw, targetYaw, 28f) - currentYaw);
        else deltaYaw = rawDeltaYaw;
        float deltaPitch = targetPitch - currentPitch;
        if (Math.abs(deltaYaw) < 0.2f && Math.abs(deltaPitch) < 0.2f) {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            applyToPlayer(currentYaw, currentPitch);
            return;
        }
        float factorYaw, factorPitch;
        float maxYaw, maxPitch;
        if(customFactor >= 0){
            float base = MathHelper.clamp(customFactor, 0.08f, 0.38f);
            // FPS-плавный: easeOutCubic + dt
            factorYaw = (1f - (float)Math.pow(1f - base, fpsFactor * 1.6f));
            factorPitch = (1f - (float)Math.pow(1f - base*0.62f, fpsFactor * 1.6f));
            maxYaw = 18f; maxPitch = 9f;
        } else {
            float s = profile.smoothing;
            float baseYaw = MathHelper.clamp(s * 0.62f, 0.08f, 0.20f);
            float basePitch = MathHelper.clamp(s * 0.42f, 0.05f, 0.12f);
            factorYaw = 1f - (float)Math.pow(1f - baseYaw, fpsFactor * 1.4f);
            factorPitch = 1f - (float)Math.pow(1f - basePitch, fpsFactor * 1.4f);
            maxYaw = switch (profile) {
                case HYPIXEL -> 8f;
                case REALLY_WORLD -> 10f;
                case FUN_TIME -> 11f;
                default -> 9f;
            };
            maxPitch = 4.2f;
        }
        float stepYaw = MathHelper.clamp(deltaYaw * factorYaw, -maxYaw, maxYaw);
        float stepPitch = MathHelper.clamp(deltaPitch * factorPitch, -maxPitch, maxPitch);
        float nextYaw = currentYaw + stepYaw;
        float nextPitch = MathHelper.clamp(currentPitch + stepPitch, -90f, 90f);
        if (gcd > 0.0001f) {
            nextYaw = Mathematics.gcdSnap(currentYaw, nextYaw, gcd);
            nextPitch = Mathematics.gcdSnap(currentPitch, nextPitch, gcd);
        }
        currentYaw = nextYaw;
        currentPitch = nextPitch;
        applyToPlayer(currentYaw, currentPitch);
    }

    private static void applyToPlayer(float yaw, float pitch) {
        exp.nefor.client.util.client.RotationUtil.setRotationRaw(yaw, pitch, true);
    }
}
