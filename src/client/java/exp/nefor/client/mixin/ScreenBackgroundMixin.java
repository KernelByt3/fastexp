package exp.nefor.client.mixin;

import exp.nefor.client.gui.CustomRenderedScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Единый чистый фон для всех ванильных меню вместо кривого dirt/panorama.
 * Ин-гейм экраны (мир есть) и наши CustomRenderedScreen не трогаем.
 */
@Mixin(Screen.class)
public class ScreenBackgroundMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void nefor$cleanBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof CustomRenderedScreen) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) return;
        Screen screen = (Screen) (Object) this;
        ci.cancel();
        context.fillGradient(0, 0, screen.width, screen.height, 0xFF121218, 0xFF07070C);
    }
}
