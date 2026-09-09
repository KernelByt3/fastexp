package exp.nefor.client.mixin;

import exp.nefor.client.event.api.EventBus;
import exp.nefor.client.event.impl.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

	@Inject(method = "onKey", at = @At("HEAD"))
	private void Nefor$onKey(long window, int action, KeyInput input, CallbackInfo info) {
		if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) return;
		EventBus.post(new KeyEvent(window, input.key(), input.scancode(), action, input.modifiers()));
	}
}
