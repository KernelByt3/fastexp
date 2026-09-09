package exp.nefor.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Точный raycast по хитбоксу — фиксит жалобы на хитбоксы.
 */
public final class RaycastUtil {
    private RaycastUtil(){}

    public static boolean canHit(LivingEntity from, LivingEntity to, double maxRange){
        var mc = MinecraftClient.getInstance();
        if(mc.world==null) return false;
        Vec3d eye = from.getEyePos();
        // ближайшая точка хитбокса к глазу
        Box box = to.getBoundingBox().expand(0.08);
        // raycast к центру хитбокса + к углам
        Vec3d target = new Vec3d(
                Math.clamp(eye.x, box.minX, box.maxX),
                Math.clamp(eye.y, box.minY, box.maxY),
                Math.clamp(eye.z, box.minZ, box.maxZ)
        );
        double dist = eye.distanceTo(target);
        if(dist > maxRange+0.3) return false;

        // проверка стен — raycast от глаз к точке хитбокса
        var hit = mc.world.raycast(new RaycastContext(eye, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, from));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static Vec3d closestPoint(Box box, Vec3d eye){
        return new Vec3d(
                Math.clamp(eye.x, box.minX, box.maxX),
                Math.clamp(eye.y, box.minY, box.maxY),
                Math.clamp(eye.z, box.minZ, box.maxZ)
        );
    }
}
