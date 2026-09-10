package exp.nefor.client.mixin;

import exp.nefor.client.gui.CustomRenderedScreen;
import exp.nefor.client.gui.UiRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Единый чистый фон для всех ванильных меню вместо кривого dirt/panorama.
 * Ин-гейм экраны (мир есть) и наши CustomRenderedScreen не трогаем.
 * Плюс клики/ESC для кастомного ConnectScreen (его Cancel рисуем сами).
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

    @Unique
    private boolean nefor$isConnect() {
        return (Object) this instanceof ConnectScreen;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void nefor$connectKeys(KeyInput ki, CallbackInfoReturnable<Boolean> cir) {
        if (!nefor$isConnect()) return;
        if (ki.key() == GLFW.GLFW_KEY_ESCAPE) {
            nefor$abortConnect();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void nefor$abortConnect() {
        ConnectScreenAccessor acc = (ConnectScreenAccessor) (Object) this;
        acc.nefor$setConnectingCancelled(true);
        try {
            if (acc.nefor$connection() != null) acc.nefor$connection().disconnect(ConnectScreen.ABORTED_TEXT);
        } catch (Exception ignored) {
        }
        MinecraftClient.getInstance().setScreen(acc.nefor$parent());
    }
}
