package exp.nefor.client.gui;

import net.minecraft.client.MinecraftClient;

import java.util.Random;

public final class SakuraBackground {

    private SakuraBackground() {
    }

    public static void draw(double timeMs) {
        var mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        double seconds = timeMs / 1000.0;

        drawSky(w, h);
        drawAurora(w, h, seconds);
        drawMoon(w, h, seconds);
        drawStars(w, h, seconds);
        drawHaze(w, h, seconds);
    }

    
    private static void drawSky(int w, int h) {
        float[][] stops = {
                {0.020f, 0.012f, 0.050f},
                {0.045f, 0.018f, 0.075f},
                {0.075f, 0.028f, 0.115f},
                {0.120f, 0.050f, 0.175f},
                {0.190f, 0.075f, 0.250f},
                {0.285f, 0.115f, 0.350f}
        };

        int strips = 96;
        for (int i = 0; i < strips; i++) {
            double t = i / (double) (strips - 1) * (stops.length - 1);
            int idx = (int) Math.min(stops.length - 2, Math.floor(t));
            double frac = t - idx;
            frac = frac * frac * (3 - 2 * frac); 

            float r = (float) lerp(stops[idx][0], stops[idx + 1][0], frac);
            float g = (float) lerp(stops[idx][1], stops[idx + 1][1], frac);
            float b = (float) lerp(stops[idx][2], stops[idx + 1][2], frac);

            UiRender.roundRect(
                    0, h * i / (double) strips,
                    w, h / (double) strips + 1.5,
                    0,
                    new float[]{r, g, b, 1.0f},
                    new float[]{0, 0, 0, 0}, 0,
                    new float[]{0, 0, 0, 0}, 0
            );
        }
    }

    
    private static void drawAurora(int w, int h, double seconds) {
        double shift1 = Math.sin(seconds * 0.16) * 18.0;
        double shift2 = Math.cos(seconds * 0.13) * 14.0;

        UiRender.roundRect(
                w * 0.58 + shift1, -h * 0.12,
                w * 0.52, h * 0.34,
                999,
                new float[]{0.76f, 0.32f, 0.98f, 0.05f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.82f, 0.38f, 1.00f, 0.18f}, 95
        );

        UiRender.roundRect(
                -w * 0.14 + shift2, h * 0.34,
                w * 0.46, h * 0.42,
                999,
                new float[]{0.58f, 0.20f, 0.80f, 0.04f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.66f, 0.28f, 0.92f, 0.14f}, 85
        );

        UiRender.roundRect(
                w * 0.26, h * 0.58,
                w * 0.62, h * 0.22,
                999,
                new float[]{0.70f, 0.34f, 0.92f, 0.025f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.75f, 0.42f, 1.00f, 0.08f}, 70
        );
    }

    
    private static void drawMoon(int w, int h, double seconds) {
        double x = w * 0.82 + Math.sin(seconds * 0.18) * 5.0;
        double y = h * 0.16 + Math.cos(seconds * 0.16) * 4.0;
        double breathe = 0.5 + 0.5 * Math.sin(seconds * 0.30);

        
        UiRender.roundRect(
                x - 86, y - 86,
                172, 172,
                999,
                new float[]{0.95f, 0.88f, 1.00f, (float) (0.03 + breathe * 0.02)},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.88f, 0.78f, 1.00f, (float) (0.12 + breathe * 0.08)}, 80
        );

        
        UiRender.roundRect(
                x - 22, y - 22,
                44, 44,
                999,
                new float[]{0.98f, 0.96f, 1.00f, 0.92f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.86f, 0.76f, 1.00f, 0.40f}, 22
        );
    }

    
    private static void drawStars(int w, int h, double seconds) {
        Random rnd = new Random(4242L);
        int count = 42;

        for (int i = 0; i < count; i++) {
            double x = rnd.nextDouble() * w;
            double y = rnd.nextDouble() * h * 0.60;
            double size = 0.9 + rnd.nextDouble() * 1.5;

            double phase = rnd.nextDouble() * Math.PI * 2.0;
            double speed = 0.6 + rnd.nextDouble() * 1.2;

            float twinkle = (float) (0.18 + 0.50 * (0.5 + 0.5 * Math.sin(seconds * speed + phase)));

            boolean lilac = rnd.nextBoolean();
            float r = lilac ? 0.95f : 1.00f;
            float g = lilac ? 0.88f : 0.97f;
            float b = lilac ? 1.00f : 0.96f;

            UiRender.roundRect(
                    x, y,
                    size, size,
                    size / 2.0,
                    new float[]{r, g, b, twinkle},
                    new float[]{0, 0, 0, 0}, 0,
                    new float[]{r, g, b, twinkle * 0.5f}, (float) (size * 3.0)
            );
        }
    }

    
    private static void drawHaze(int w, int h, double seconds) {
        double wave = Math.sin(seconds * 0.10) * 8.0;

        UiRender.roundRect(
                -w * 0.10, h * 0.72 + wave * 0.2,
                w * 1.20, h * 0.34,
                999,
                new float[]{0.18f, 0.06f, 0.28f, 0.05f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0.60f, 0.25f, 0.85f, 0.12f}, 95
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}