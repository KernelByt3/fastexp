package exp.nefor.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Брендинг захода на сервер: nefor client + progress bar.
 */
@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void nefor$brand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ConnectScreen screen = (ConnectScreen) (Object) this;
        int w = screen.width;
        int h = screen.height;
        var text = MinecraftClient.getInstance().textRenderer;

        context.drawCenteredTextWithShadow(text, "nefor client", w / 2, h / 2 - 64, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(text, "joining server", w / 2, h / 2 - 48, 0xFF6E6E78);

        int bw = 180;
        int bx = w / 2 - bw / 2;
        int by = h / 2 + 24;
        context.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF2A2A33);
        context.fill(bx, by, bx + bw, by + 6, 0xFF141419);
        int seg = 54;
        long t = System.currentTimeMillis() % 1200;
        int sx = bx + (int) ((bw - seg) * t / 1200);
        context.fill(sx, by, sx + seg, by + 6, 0xFFFFFFFF);
    }
}
