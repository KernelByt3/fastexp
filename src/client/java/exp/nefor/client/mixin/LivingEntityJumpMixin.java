package exp.nefor.client.mixin;

import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Unique
    private float nefor$originalJumpYaw;

    @Inject(method = "jump()V", at = @At("HEAD"))
    private void onJumpHead(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (isLocalRotatingPlayer(self)) {
            nefor$originalJumpYaw = self.getYaw();
            self.setYaw(RotationUtil.targetYaw);
        }
    }

    @Inject(method = "jump()V", at = @At("RETURN"))
    private void onJumpReturn(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (isLocalRotatingPlayer(self)) {
            self.setYaw(nefor$originalJumpYaw);
        }
    }

    private static boolean isLocalRotatingPlayer(LivingEntity entity) {
        return entity == MinecraftClient.getInstance().player
                && RotationUtil.isRotating
                && RotationUtil.moveCorrection;
    }
}