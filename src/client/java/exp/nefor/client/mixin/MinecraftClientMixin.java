package exp.nefor.client.mixin;

import exp.nefor.client.event.api.EventBus;
import exp.nefor.client.event.impl.ClientTickEvent;
import exp.nefor.client.event.impl.HudRenderEvent;
import exp.nefor.client.gui.NeforTitleScreen;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void nefor$replaceTitleScreen(Screen screen, CallbackInfo info) {
		if (screen != null && screen.getClass() == TitleScreen.class) {
			info.cancel();
			((MinecraftClient) (Object) this).setScreen(new NeforTitleScreen());
		}
	}

	@Inject(method = "render", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/util/Window;swapBuffers(Lnet/minecraft/client/util/tracy/TracyFrameCapturer;)V",
			shift = At.Shift.BEFORE
	))
	private void fastexp$renderAfterGameBlit(boolean tick, CallbackInfo info) {
		EventBus.post(new HudRenderEvent());

		MinecraftClient client = (MinecraftClient) (Object) this;

		// доводка каждый кадр (155fps), а не 20 TPS — иначе ступеньки как у робота
		exp.nefor.client.system.rotation.SmoothRotationManager.tick();

		RenderSystem.render();

		if (client.currentScreen instanceof exp.nefor.client.gui.CustomRenderedScreen screen) {
			screen.nefor$renderOverlay();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void fastexp$beforeTick(CallbackInfo info) {
		EventBus.post(new ClientTickEvent(MinecraftClient.getInstance()));
	}
}