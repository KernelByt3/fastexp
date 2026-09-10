package exp.nefor.client.gui;

import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;

public final class UiRender {

    private UiRender() {
    }

    public static int fbWidth() {
        return MinecraftClient.getInstance().getWindow().getFramebufferWidth();
    }

    public static int fbHeight() {
        return MinecraftClient.getInstance().getWindow().getFramebufferHeight();
    }

    /** Rounded rect in logical (GUI) coordinates; scaling handled here. */
    public static void roundRect(double x, double y, double w, double h, double radius,
                                 float[] fill, float[] outline, float outlineWidth,
                                 float[] glow, float glowSize) {
        float scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        RenderSystem.drawRoundedRect(
                (float) (x * scale), (float) (y * scale),
                (float) (w * scale), (float) (h * scale),
                (float) (radius * scale),
                fbWidth(), fbHeight(),
                fill, outline, outlineWidth * scale, glow, glowSize * scale);
    }

    public static void dimScreen(int width, int height, float alpha) {
        roundRect(0, 0, width, height, 0,
                new float[]{0.04f, 0.03f, 0.08f, alpha},
                new float[]{1, 1, 1, 0.08f}, 1,
                new float[]{0.54f, 0.17f, 0.89f, 0.12f}, 5);
    }

    /** Непрозрачная подложка для наших экранов — перекрывает панораму/мир. */
    public static void background(int width, int height) {
        roundRect(0, 0, width, height, 0,
                new float[]{0.055f, 0.055f, 0.085f, 1f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0, 0, 0, 0}, 0);
    }

    public static void panel(double x, double y, double w, double h) {
        roundRect(x, y, w, h, 8,
                new float[]{0.07f, 0.06f, 0.12f, 0.96f},
                new float[]{1, 1, 1, 0.08f}, 1,
                new float[]{0.54f, 0.17f, 0.89f, 0.12f}, 5);
    }

    public static void field(double x, double y, double w, double h, boolean hovered) {
        roundRect(x, y, w, h, 5,
                new float[]{0.05f, 0.04f, 0.09f, 0.95f},
                new float[]{1, 1, 1, hovered ? 0.25f : 0.10f}, 1,
                new float[]{0.54f, 0.17f, 0.89f, 0.0f}, 0);
    }

    public static void row(double x, double y, double w, double h, boolean selected, boolean hovered) {
        float[] fill = selected
                ? new float[]{0.35f, 0.11f, 0.56f, 0.85f}
                : hovered ? new float[]{0.14f, 0.13f, 0.22f, 0.85f} : new float[]{0.10f, 0.09f, 0.16f, 0.80f};
        roundRect(x, y, w, h, 5,
                fill,
                new float[]{1, 1, 1, selected ? 0.25f : 0.05f}, 1,
                new float[]{0, 0, 0, 0}, 0);
    }

    public static void button(double x, double y, double w, double h, String label, boolean hovered, boolean danger) {
        float[] fill = hovered
                ? (danger ? new float[]{0.45f, 0.10f, 0.15f, 0.95f} : new float[]{0.35f, 0.11f, 0.56f, 0.95f})
                : new float[]{0.10f, 0.09f, 0.16f, 0.92f};
        float[] glow = hovered ? new float[]{0.54f, 0.17f, 0.89f, 0.22f} : new float[]{0, 0, 0, 0};
        roundRect(x, y, w, h, 7,
                fill,
                new float[]{1, 1, 1, hovered ? 0.20f : 0.07f}, 1,
                glow, hovered ? 3.5f : 0);

        int color = hovered ? 0xFFFFFFFF : 0xFFE6E6E6;
        float size = 14.0f;
        float textX = (float) (x + (w - RenderSystem.textWidth(label, size)) / 2.0f);
        float textY = (float) (y + (h - size) / 2.0f + 1.0f);
        RenderSystem.drawText(label, textX, textY, size, color);
    }

    public static boolean inBox(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + h;
    }
}
