package exp.nefor.client.mixin;

import exp.nefor.client.gui.CustomRenderedScreen;
import exp.nefor.client.gui.UiRender;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Заход на сервер полностью через наш рендер: фон, nefor client,
 * статус, progress bar, кнопка отмены. Ваниллу гасим целиком.
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin implements CustomRenderedScreen {

    @Shadow
    volatile ClientConnection connection;
    @Shadow
    volatile boolean connectingCancelled;
    @Shadow
    @Final
    Screen parent;
    @Shadow
    private Text status;

    @Unique
    private final long nefor$openMs = System.currentTimeMillis();
    @Unique
    private boolean nefor$widgetsCleared = false;

    @Unique
    private static int nefor$cancelX(int w) { return w / 2 - 90; }
    @Unique
    private static int nefor$cancelY(int h) { return h / 2 + 44; }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nefor$killVanilla(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        if (!nefor$widgetsCleared) {
            nefor$widgetsCleared = true;
            ((ScreenAccessor) (Object) this).nefor$clearChildren();
        }
    }

    @Unique
    private void nefor$abort() {
        connectingCancelled = true;
        try {
            if (connection != null) connection.disconnect(ConnectScreen.ABORTED_TEXT);
        } catch (Exception ignored) {
        }
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void nefor$click(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        if (UiRender.inBox(click.x(), click.y(), nefor$cancelX(w), nefor$cancelY(h), 180, 22)) {
            nefor$abort();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void nefor$keys(KeyInput ki, CallbackInfoReturnable<Boolean> cir) {
        if (ki.key() == GLFW.GLFW_KEY_ESCAPE) {
            nefor$abort();
            cir.setReturnValue(true);
        }
    }

    @Override
    public void nefor$renderOverlay() {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        double mx = client.mouse.getX() / client.getWindow().getScaleFactor();
        double my = client.mouse.getY() / client.getWindow().getScaleFactor();

        UiRender.background(w, h);

        float titleE = exp.nefor.client.util.AnimationUtil.openT(nefor$openMs, 0L, 320L);
        String title = "nefor client";
        RenderSystem.drawText(title, w / 2f - RenderSystem.textWidth(title, 24f) / 2f,
                h / 2f - 66f - (1f - titleE) * 14f, 24f, 0xFFFFFFFF);

        String st = status != null ? status.getString() : "connecting...";
        if (st.length() > 48) st = st.substring(0, 48);
        RenderSystem.drawText(st, w / 2f - RenderSystem.textWidth(st, 11f) / 2f, h / 2f - 36f, 11f, 0xFF9A9AA5);

        float barE = exp.nefor.client.util.AnimationUtil.openT(nefor$openMs, 120L, 350L);
        float bw = 180f * (0.3f + 0.7f * barE);
        float bx = w / 2f - bw / 2f;
        float by = h / 2f + 8f;
        UiRender.roundRect(bx, by, bw, 6, 3,
                new float[]{1, 1, 1, 0.10f}, new float[]{1, 1, 1, 0.12f}, 1,
                new float[]{0, 0, 0, 0}, 0);
        float seg = Math.min(54f, bw);
        long t = System.currentTimeMillis() % 1200;
        float sx = bx + (bw - seg) * t / 1200f;
        UiRender.roundRect(sx, by, seg, 6, 3,
                new float[]{1, 1, 1, 0.9f}, new float[]{0, 0, 0, 0}, 0,
                new float[]{0, 0, 0, 0}, 0);

        boolean hov = UiRender.inBox(mx, my, nefor$cancelX(w), nefor$cancelY(h), 180, 22);
        UiRender.button(nefor$cancelX(w), nefor$cancelY(h), 180, 22, "Cancel", hov, false);
    }
}
