package exp.nefor.client.util.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean isRotating = false;
    public static boolean moveCorrection = false;

    public static float targetYaw;
    public static float targetPitch;

    public static float prevTargetYaw;
    public static float prevTargetPitch;

    public static float prevPlayerYaw;
    public static float prevPlayerPitch;

    private static float lastRealYaw = 0.0f;
    private static float lastRealPitch = 0.0f;
    private static double sensitivityGcd = 0.0;
    private static int gcdSampleCount = 0;
    private static final int GCD_MIN_SAMPLES = 5;

    private static Vec3d lockedAimPoint = null;
    private static Entity lockedTarget = null;

    
    
    private static Entity smoothTarget = null;
    private static float smoothYaw = 0f;
    private static float smoothPitch = 0f;
    private static final float AIM_SMOOTH = 0.6f;


    public static void setRotation(float yaw, float pitch, boolean moveCorrectionEnabled) {
        yaw = applyGcdNormalization(yaw);
        pitch = applyGcdNormalization(pitch);
        setRotationRaw(yaw, pitch, moveCorrectionEnabled);
    }

    
    public static void setRotationRaw(float yaw, float pitch, boolean moveCorrectionEnabled) {
        prevTargetYaw = isRotating ? targetYaw : (mc.player != null ? mc.player.getYaw() : yaw);
        prevTargetPitch = isRotating ? targetPitch : (mc.player != null ? mc.player.getPitch() : pitch);
        targetYaw = yaw; 
        targetPitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        moveCorrection = moveCorrectionEnabled;
        isRotating = true;
    }

    public static void reset() {
        isRotating = false;
        moveCorrection = false;
        lockedAimPoint = null;
        lockedTarget = null;
        smoothTarget = null;
    }

    public static void onClientTick() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        if (!isRotating) {
            float deltaYaw = Math.abs(MathHelper.wrapDegrees(currentYaw - lastRealYaw));
            float deltaPitch = Math.abs(currentPitch - lastRealPitch);

            if (deltaYaw > 0.0001f) {
                sensitivityGcd = gcdSampleCount == 0
                        ? deltaYaw
                        : gcd(sensitivityGcd, deltaYaw);
                gcdSampleCount = Math.min(gcdSampleCount + 1, 100);
            }
            if (deltaPitch > 0.0001f) {
                sensitivityGcd = gcdSampleCount == 0
                        ? deltaPitch
                        : gcd(sensitivityGcd, deltaPitch);
                gcdSampleCount = Math.min(gcdSampleCount + 1, 100);
            }
        }

        lastRealYaw = currentYaw;
        lastRealPitch = currentPitch;

        if (isRotating) {
            prevTargetYaw = targetYaw;
            prevTargetPitch = targetPitch;
        } else {
            prevTargetYaw = currentYaw;
            prevTargetPitch = currentPitch;
            targetYaw = currentYaw;
            targetPitch = currentPitch;
        }
    }
    public static float[] getRotations(Entity entity) {
        return getRotations(entity, 0.0);
    }

    



    public static float[] getRotations(Entity entity, double leadSec) {
        if (mc.player == null || entity == null) return new float[]{0.0f, 0.0f};

        Box box = entity.getBoundingBox();
        if (leadSec > 0) {
            Vec3d vel = entity.getVelocity();
            if (vel.lengthSquared() > 0.0004) {
                box = box.offset(vel.x * leadSec, vel.y * leadSec, vel.z * leadSec);
            }
        }

        
        float[] raw = calculateAngles(headPoint(box));
        lockedAimPoint = headPoint(box);
        lockedTarget = entity;

        
        
        if (entity != smoothTarget) {
            smoothTarget = entity;
            smoothYaw = raw[0];
            smoothPitch = raw[1];
        } else {
            smoothYaw = raw[0];
            smoothPitch += (raw[1] - smoothPitch) * AIM_SMOOTH;
        }
        return new float[]{smoothYaw, MathHelper.clamp(smoothPitch, -90.0F, 90.0F)};
    }
    public static void updateLockedPoint(Entity entity) {
        if (entity == null) return;
        
        lockedAimPoint = headPoint(entity);
        lockedTarget = entity;
    }

    private static Vec3d headPoint(Entity entity) {
        return headPoint(entity.getBoundingBox());
    }

    private static Vec3d headPoint(Box box) {
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        
        double y = Math.max(box.maxY - 0.15, (box.minY + box.maxY) * 0.5);
        return new Vec3d(cx, y, cz);
    }

    private static Vec3d selectBestPoint(Entity entity) {
        return headPoint(entity);
    }

    private static float[] calculateAngles(Vec3d targetVec) {
        if (mc.player == null) return new float[]{0.0f, 0.0f};

        Vec3d eyes = mc.player.getEyePos();
        double diffX = targetVec.x - eyes.x;
        double diffY = targetVec.y - eyes.y;
        double diffZ = targetVec.z - eyes.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{
                MathHelper.wrapDegrees(yaw),
                MathHelper.clamp(pitch, -90.0F, 90.0F)
        };
    }

    public static boolean isLookingAt(Entity entity, float maxAngle) {
        if (entity == null || mc.player == null) return false;

        
        Vec3d checkPoint = headPoint(entity);

        float[] needed = calculateAngles(checkPoint);
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(targetYaw - needed[0]));
        float deltaPitch = Math.abs(targetPitch - needed[1]);

        return Math.hypot(deltaYaw, deltaPitch) <= maxAngle;
    }

    private static float applyGcdNormalization(float targetAngle) {
        if (mc.player == null || gcdSampleCount < GCD_MIN_SAMPLES || sensitivityGcd < 0.0001) {
            return targetAngle;
        }

        float base = isRotating ? targetYaw : mc.player.getYaw();
        float delta = MathHelper.wrapDegrees(targetAngle - base);

        double steps = delta / sensitivityGcd;
        long roundedSteps = Math.round(steps);

        return MathHelper.wrapDegrees(base + (float) (roundedSteps * sensitivityGcd));
    }

    private static double gcd(double a, double b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 0.0001) {
            double t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}