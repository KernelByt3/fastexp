package exp.nefor.client.mixin;

import exp.nefor.client.gui.CustomRenderedScreen;
import exp.nefor.client.gui.UiRender;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Брендинг захода на сервер через наш рендер: nefor client + progress bar.
 * Ванильный render (статус + кнопка отмены) остаётся под оверлеем.
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin implements CustomRenderedScreen {

    @Unique
    private long nefor$openMs = System.currentTimeMillis();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nefor$stampOpen(CallbackInfo ci) {
        nefor$openMs = System.currentTimeMillis();
    }

    @Override
    public void nefor$renderOverlay() {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        long openMs = nefor$openMs;

        float titleE = exp.nefor.client.util.AnimationUtil.openT(openMs, 0L, 320L);
        String title = "nefor client";
        RenderSystem.drawText(title, w / 2f - RenderSystem.textWidth(title, 24f) / 2f,
                h / 2f - 66f - (1f - titleE) * 14f, 24f, 0xFFFFFFFF);

        float barE = exp.nefor.client.util.AnimationUtil.openT(openMs, 120L, 350L);
        float bw = 180f * (0.3f + 0.7f * barE);
        float bx = w / 2f - bw / 2f;
        float by = h / 2f + 26f;
        UiRender.roundRect(bx, by, bw, 6, 3,
                new float[]{1, 1, 1, 0.10f}, new float[]{1, 1, 1, 0.12f}, 1,
                new float[]{0, 0, 0, 0}, 0);
        float seg = 54f;
        long t = System.currentTimeMillis() % 1200;
        float sx = bx + (bw - seg) * t / 1200f;
        UiRender.roundRect(sx, by, seg, 6, 3,
                new float[]{1, 1, 1, 0.9f}, new float[]{0, 0, 0, 0}, 0,
                new float[]{0, 0, 0, 0}, 0);
    }
}
