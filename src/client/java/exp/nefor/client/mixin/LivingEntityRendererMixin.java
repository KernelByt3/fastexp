package exp.nefor.client.mixin;

import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Unique
    private float originalPitch;
    @Unique
    private float originalHeadYaw;
    @Unique
    private float originalLastHeadYaw;
    @Unique
    private float originalBodyYaw;
    @Unique
    private float originalLastBodyYaw;

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD")
    )
    private void onUpdateRenderStateHead(LivingEntity livingEntity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (livingEntity == MinecraftClient.getInstance().player && RotationUtil.isRotating) {
            // 1. Сохраняем реальные локальные углы игрока
            originalPitch = livingEntity.getPitch();
            originalHeadYaw = livingEntity.headYaw;
            originalLastHeadYaw = livingEntity.lastHeadYaw;
            originalBodyYaw = livingEntity.bodyYaw;
            originalLastBodyYaw = livingEntity.lastBodyYaw;

            // 2. Подменяем углы на целевые (игра сама рассчитает интерполяцию для кадра)
            livingEntity.setPitch(RotationUtil.targetPitch);
            livingEntity.headYaw = RotationUtil.targetYaw;
            livingEntity.lastHeadYaw = RotationUtil.prevTargetYaw;
            livingEntity.bodyYaw = RotationUtil.targetYaw;
            livingEntity.lastBodyYaw = RotationUtil.prevTargetYaw;
        }
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void onUpdateRenderStateReturn(LivingEntity livingEntity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (livingEntity == MinecraftClient.getInstance().player && RotationUtil.isRotating) {
            // 3. Сразу после сбора кадра возвращаем реальные клиентские углы назад
            livingEntity.setPitch(originalPitch);
            livingEntity.headYaw = originalHeadYaw;
            livingEntity.lastHeadYaw = originalLastHeadYaw;
            livingEntity.bodyYaw = originalBodyYaw;
            livingEntity.lastBodyYaw = originalLastBodyYaw;
        }
    }
}