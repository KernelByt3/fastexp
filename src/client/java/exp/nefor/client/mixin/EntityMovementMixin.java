package exp.nefor.client.mixin;

import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityMovementMixin {

    @Redirect(
            method = "updateVelocity(FLnet/minecraft/util/math/Vec3d;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F")
    )
    private float redirectYawForVelocity(Entity self) {
        if (isLocalRotatingPlayer(self)) {
            return RotationUtil.targetYaw;
        }
        return self.getYaw();
    }

    private static boolean isLocalRotatingPlayer(Entity entity) {
        return entity == MinecraftClient.getInstance().player
                && RotationUtil.isRotating
                && RotationUtil.moveCorrection;
    }
}