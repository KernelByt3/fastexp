package exp.nefor.client.mixin;

import exp.nefor.client.util.client.MultiActionsBypass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySlowMixin {
    // отключено для Grim — гасило скорость и давало Simulation .42 / GroundSpoof
    // @Inject(method = "tickMovement", at = @At("TAIL"))
    // private void nefor$slowForMultiActions(CallbackInfo ci) { ... }
}
