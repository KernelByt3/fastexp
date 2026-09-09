package exp.nefor.client.util.client;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public final class MoveFixUtil {

    private MoveFixUtil() {}

    public static Vec2f correctMovement(Vec2f movement, float cameraYaw, float spoofedYaw) {
        if (movement.lengthSquared() < 1.0E-5F) return movement;

        float rad = (float) Math.toRadians(MathHelper.wrapDegrees(cameraYaw - spoofedYaw));
        float cos = MathHelper.cos(rad);
        float sin = MathHelper.sin(rad);

        float strafe = movement.x;
        float forward = movement.y;

        float newStrafe = strafe * cos - forward * sin;
        float newForward = forward * cos + strafe * sin;

        return new Vec2f(
                Math.round(newStrafe * 10000.0f) / 10000.0f,
                Math.round(newForward * 10000.0f) / 10000.0f
        );
    }
}