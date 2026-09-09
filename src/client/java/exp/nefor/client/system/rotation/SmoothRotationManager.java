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
    // плавный возврат взгляда: после потери цели глайд к камере вместо снапа
    private static boolean returning = false;
    private static long returnStart = 0;

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
        returning = false;
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
        returning = false;
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
        returning = false;
        applyToPlayer(currentYaw, currentPitch);
    }

    public static void reset() {
        active = false;
        returning = false;
        velocityYaw = velocityPitch = 0;
        lastUpdate = 0;
        customFactor = -1f;
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        exp.nefor.client.util.client.RotationUtil.reset();
    }

    /**
     * Плавное отворачивание: вместо мгновенного сброса к камере ставит
     * камеру целью глайда. Тик-пакеты продолжают нести плавный поворот —
     * со стороны сервера всё легитимно, снапа нет.
     */
    public static void release() {
        if (!active || mc.player == null) { reset(); return; }
        float camYaw = mc.player.getYaw();
        float delta = MathHelper.wrapDegrees(camYaw - MathHelper.wrapDegrees(currentYaw));
        targetYaw = currentYaw + delta;
        targetPitch = MathHelper.clamp(mc.player.getPitch(), -90f, 90f);
        lastUpdate = System.currentTimeMillis();
        returning = true;
        returnStart = lastUpdate;
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
        if (returning && System.currentTimeMillis() - returnStart > 500) { reset(); return; }

        long nowNs = System.nanoTime();
        float dt = (nowNs - lastTickNs) / 1_000_000_000f;
        lastTickNs = nowNs;
        dt = MathHelper.clamp(dt, 0.005f, 0.05f); // 5-50ms
        float fpsFactor = dt * 20f; // нормируем к 20 TPS

        // GCD всегда: камера в ванилле двигается только кратно GCD,
        // иначе Grim AimModulo360. Чистый снап без шума (Mathematics.gcdSnap
        // подмешивает рандом и рвёт сетку — не использовать здесь).
        float gcd = GcdUtil.getGcd();
        float rawDeltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaYaw = rawDeltaYaw;
        float deltaPitch = targetPitch - currentPitch;
        if (Math.abs(deltaYaw) < 0.2f && Math.abs(deltaPitch) < 0.2f) {
            if (returning) { reset(); return; } // взгляд вернулся к камере
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
            maxYaw = 26f; maxPitch = 14f;
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
                default -> 14f;
            };
            maxPitch = 8f;
        }
        float stepYaw = MathHelper.clamp(deltaYaw * factorYaw, -maxYaw, maxYaw);
        float stepPitch = MathHelper.clamp(deltaPitch * factorPitch, -maxPitch, maxPitch);
        float nextYaw = currentYaw + stepYaw;
        float nextPitch = MathHelper.clamp(currentPitch + stepPitch, -90f, 90f);
        if (gcd > 0.0001f) {
            nextYaw = GcdUtil.snapAngle(currentYaw, nextYaw, gcd);
            nextPitch = GcdUtil.snapAngle(currentPitch, nextPitch, gcd);
        }
        currentYaw = nextYaw;
        currentPitch = nextPitch;
        applyToPlayer(currentYaw, currentPitch);
    }

    private static void applyToPlayer(float yaw, float pitch) {
        exp.nefor.client.util.client.RotationUtil.setRotationRaw(yaw, pitch, true);
    }
}
