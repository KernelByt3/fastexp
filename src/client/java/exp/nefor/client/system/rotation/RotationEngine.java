package exp.nefor.client.system.rotation;

import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;





public final class RotationEngine {

    private static long lastNoiseTime = 0;
    private static float cachedYawNoise = 0;
    private static float cachedPitchNoise = 0;

    private RotationEngine() {}

    public static void rotateTo(LivingEntity target, RotationProfile profile) {
        rotateTo(target, profile, 0.0);
    }

    public static void rotateTo(LivingEntity target, RotationProfile profile, double leadSec) {
        if (target == null) return;

        float[] base = RotationUtil.getRotations(target, leadSec);

        long now = System.currentTimeMillis();
        if (now - lastNoiseTime > 400) {
            float nYaw = profile.noiseYaw;
            float nPitch = nYaw * profile.noisePitchScale;
            cachedYawNoise = (float)(ThreadLocalRandom.current().nextGaussian() * nYaw * 0.30);
            
            cachedPitchNoise = (float)(ThreadLocalRandom.current().nextGaussian() * nPitch * 0.09);
            lastNoiseTime = now;
        }

        float yaw = MathHelper.wrapDegrees(base[0] + cachedYawNoise);
        float pitch = MathHelper.clamp(base[1] + cachedPitchNoise, -90f, 90f);

        SmoothRotationManager.setTarget(yaw, pitch, profile);
    }

    public static boolean isAimed(LivingEntity target, RotationProfile profile) {
        return RotationUtil.isLookingAt(target, profile.fovCheck);
    }
}
