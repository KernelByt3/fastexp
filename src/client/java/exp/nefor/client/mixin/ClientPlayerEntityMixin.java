package exp.nefor.client.mixin;

import com.mojang.authlib.GameProfile;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    private ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void nefor$sendMovementHead(CallbackInfo ci) {
        if (((Object) this) == MinecraftClient.getInstance().player && RotationUtil.isRotating) {
            RotationUtil.prevPlayerYaw = getYaw();
            RotationUtil.prevPlayerPitch = getPitch();
            setYaw(RotationUtil.targetYaw);
            setPitch(RotationUtil.targetPitch);
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void nefor$sendMovementReturn(CallbackInfo ci) {
        if (((Object) this) == MinecraftClient.getInstance().player && RotationUtil.isRotating) {
            setYaw(RotationUtil.prevPlayerYaw);
            setPitch(RotationUtil.prevPlayerPitch);
        }
    }
}
