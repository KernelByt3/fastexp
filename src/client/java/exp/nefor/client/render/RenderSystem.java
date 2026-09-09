package exp.nefor.client.render;

import exp.nefor.client.render.font.Fonts;
import exp.nefor.client.util.data.NotificationData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBTruetype.*;

public final class RenderSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("nefor/gl-text");

    private static final int LATIN_FIRST = 0x20;
    private static final int LATIN_COUNT = 0x100 - LATIN_FIRST;
    private static final int CYRILLIC_FIRST = 0x400;
    private static final int CYRILLIC_COUNT = 0x100;
    private static final int OVERSAMPLE = 3;
    private static final int[] ATLAS_SIZES = {512, 1024, 2048, 4096, 8192};
    private static final int MIN_PIXEL_HEIGHT = 6;
    private static final int MAX_PIXEL_HEIGHT = 256;

    private static final String VERTEX_SHADER = """
            #version 150 core
            in vec2 position;
            in vec2 uv;
            uniform vec2 viewport;
            out vec2 texCoord;
            void main() {
                vec2 clip = vec2(position.x / viewport.x * 2.0 - 1.0,
                                 1.0 - position.y / viewport.y * 2.0);
                gl_Position = vec4(clip, 0.0, 1.0);
                texCoord = uv;
            }
            """;
    private static final String FRAGMENT_SHADER = """
            #version 150 core
            uniform sampler2D fontAtlas;
            uniform vec4 textColor;
            in vec2 texCoord;
            out vec4 color;
            void main() {
                float coverage = texture(fontAtlas, texCoord).r;
                coverage = pow(coverage, 0.8);
                color = vec4(textColor.rgb, textColor.a * coverage);
            }
            """;
    private static final String ROUNDED_FRAGMENT_SHADER = """
            #version 150 core
            uniform vec2 viewport;
            uniform vec4 rect;
            uniform float radius;
            uniform vec4 fillColor;
            uniform vec4 outlineColor;
            uniform float outlineWidth;
            uniform vec4 glowColor;
            uniform float glowSize;
            out vec4 color;
            void main() {
                vec2 pixel = vec2(gl_FragCoord.x, viewport.y - gl_FragCoord.y);
                vec2 halfSize = rect.zw * 0.5;
                vec2 p = abs(pixel - rect.xy - halfSize) - halfSize + radius;
                float d = length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - radius;

                float aa = 1.25;
                float fill = clamp(0.5 - d / aa, 0.0, 1.0);

                float halfW = outlineWidth * 0.5;
                float outlineMask = outlineWidth > 0.0
                        ? clamp(0.5 - (abs(d + halfW) - halfW) / aa, 0.0, 1.0)
                        : 0.0;

                float glow = glowSize > 0.0 && d > 0.0 ? exp(-d / glowSize) * glowColor.a : 0.0;

                vec4 result = vec4(glowColor.rgb, glow);
                result = mix(result, outlineColor, outlineMask * outlineColor.a);
                result = mix(result, fillColor, fill * fillColor.a);
                color = result;
            }
            """;
    private static final String TEXTURED_FRAGMENT_SHADER = """
            #version 150 core
            uniform sampler2D iconTex;
            in vec2 texCoord;
            out vec4 color;
            void main() {
                color = texture(iconTex, texCoord);
            }
            """;

    private record Atlas(String fontId, int texture, int size,
                         STBTTPackedchar.Buffer latin,
                         STBTTPackedchar.Buffer cyrillic, float baseline) {}

    private static final Map<String, Atlas> ATLASES = new HashMap<>();
    private static final Map<String, ByteBuffer> FONT_DATA = new HashMap<>();
    private static final Map<String, STBTTFontinfo> FONT_INFO = new HashMap<>();
    private static final Map<String, Integer> ICONS = new HashMap<>();
    private static final Map<String, int[]> ICON_SIZES = new HashMap<>();

    private static final List<NotificationData> NOTIFICATIONS = new ArrayList<>();
    private static final long NOTIFICATION_DEFAULT_MS = 3000;
    private static final float NOTIFICATION_FADE_IN_MS = 150.0f;
    private static final float NOTIFICATION_FADE_OUT_MS = 250.0f;

    private static String activeFont = "inter";

    private static boolean initialized;
    private static int program;
    private static int roundedProgram;
    private static int texturedProgram;
    private static int texturedViewportUniform;
    private static int texturedTexUniform;
    private static int vao;
    private static int vbo;
    private static int viewportUniform;
    private static int colorUniform;
    private static int atlasUniform;
    private static int roundedViewportUniform;
    private static int roundedRectUniform;
    private static int roundedRadiusUniform;
    private static int roundedFillUniform;
    private static int roundedOutlineUniform;
    private static int roundedOutlineWidthUniform;
    private static int roundedGlowUniform;
    private static int roundedGlowSizeUniform;

    private RenderSystem() {}

    public static String getActiveFont() {
        return activeFont;
    }

    public static void setActiveFont(String fontId) {
        if (Fonts.get(fontId).id().equals(fontId)) {
            activeFont = fontId;
        }
    }

    public static void notification(String text, int color) {
        notification(text, color, NOTIFICATION_DEFAULT_MS);
    }

    public static void notification(String text, int color, long durationMs) {
        NOTIFICATIONS.add(new NotificationData(text, color, durationMs));
    }

    public static void render() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (!initialized) initialize();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Iterator<NotificationData> iterator = NOTIFICATIONS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) iterator.remove();
        }
        if (NOTIFICATIONS.isEmpty()) return;

        float scale = (float) client.getWindow().getScaleFactor();
        int fbWidth = client.getWindow().getFramebufferWidth();
        int fbHeight = client.getWindow().getFramebufferHeight();
        float scaledWidth = client.getWindow().getScaledWidth();
        float scaledHeight = client.getWindow().getScaledHeight();

        int slot = 0;
        for (NotificationData notification : NOTIFICATIONS) {
            drawNotification(notification, slot++, scale, fbWidth, fbHeight, scaledWidth, scaledHeight);
        }
    }

    private static void drawNotification(NotificationData notification, int slot,
                                         float scale, int fbWidth, int fbHeight,
                                         float scaledWidth, float scaledHeight) {
        long elapsed = System.currentTimeMillis() - notification.startTime;
        float fadeIn = Math.min(1.0f, elapsed / NOTIFICATION_FADE_IN_MS);
        float fadeOut = Math.min(1.0f,
                Math.max(0.0f, (notification.durationMs - elapsed) / NOTIFICATION_FADE_OUT_MS));
        float alpha = Math.min(fadeIn, fadeOut);

        float centerX = scaledWidth / 2.0f;
        float centerY = scaledHeight / 2.0f;

        float boxHeight = 20.0f;
        float boxRadius = 8.0f;
        float boxSpacing = 4.0f;
        float offsetCrosshair = 80.0f;
        float textSize = 12.0f;
        float paddingX = 10.0f;
        float paddingBottom = 3.0f;
        float minBoxWidth = 100.0f;

        String text = notification.text;
        String font = activeFont;
        float drawnWidth = measureTextPixels(text, font, textSize * scale) / scale;
        float boxWidth = Math.max(minBoxWidth, drawnWidth + paddingX * 2.0f);

        float rectX = centerX + offsetCrosshair;
        float rectY = centerY - (boxHeight / 2.0f) + slot * (boxHeight + boxSpacing);

        drawRoundedRect(
                rectX * scale, rectY * scale,
                boxWidth * scale, boxHeight * scale,
                boxRadius * scale,
                fbWidth, fbHeight,
                new float[]{0.0f, 0.0f, 0.0f, 0.75f * alpha},
                new float[]{1.0f, 1.0f, 1.0f, 0.10f * alpha}, 1.0f * scale,
                new float[]{0.54f, 0.17f, 0.89f, 0.22f * alpha}, 4.0f * scale
        );

        int argb = notification.color;
        int fadedAlpha = (int) (((argb >>> 24) & 0xFF) / 255.0f * alpha * 255.0f + 0.5f);
        drawText(font, text,
                rectX + paddingX,
                rectY + boxHeight - textSize - paddingBottom,
                textSize,
                (fadedAlpha << 24) | (argb & 0x00FFFFFF));
    }

    private static float measureTextPixels(String text, String fontId, float sizeInPixels) {
        if (text.isEmpty()) return 0.0f;
        Atlas atlas = getAtlas(fontId, pixelHeight(sizeInPixels));

        FloatBuffer penX = BufferUtils.createFloatBuffer(1);
        FloatBuffer penY = BufferUtils.createFloatBuffer(1);
        float originX = 0.0f;
        penX.put(0, originX);
        penY.put(0, 0.0f);
        float right = originX;
        try (STBTTAlignedQuad quad = STBTTAlignedQuad.malloc()) {
            for (int i = 0; i < text.length(); i++) {
                int codePoint = text.charAt(i);
                STBTTPackedchar.Buffer range;
                int index;
                if (codePoint >= LATIN_FIRST && codePoint < LATIN_FIRST + LATIN_COUNT) {
                    range = atlas.latin();
                    index = codePoint - LATIN_FIRST;
                } else if (codePoint >= CYRILLIC_FIRST && codePoint < CYRILLIC_FIRST + CYRILLIC_COUNT) {
                    range = atlas.cyrillic();
                    index = codePoint - CYRILLIC_FIRST;
                } else {
                    range = atlas.latin();
                    index = '?' - LATIN_FIRST;
                }
                stbtt_GetPackedQuad(range, atlas.size(), atlas.size(), index, penX, penY, quad, true);
                right = Math.max(right, quad.x1());
            }
        }
        return right - originX;
    }

    public static void drawText(String text, float x, float y, float size, int argb) {
        drawText(activeFont, text, x, y, size, argb);
    }

    public static void drawText(String fontId, String text, float x, float y, float size, int argb) {
        if (!initialized) initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        float scale = (float) client.getWindow().getScaleFactor();
        drawTextPixels(text, fontId, x * scale, y * scale, size * scale,
                client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(),
                ((argb >>> 16) & 0xFF) / 255.0f, ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f, ((argb >>> 24) & 0xFF) / 255.0f);
    }

    public static float textWidth(String text, float size) {
        return textWidth(activeFont, text, size);
    }

    public static float textWidth(String fontId, String text, float size) {
        if (!initialized) initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        float scale = (float) client.getWindow().getScaleFactor();
        Atlas atlas = getAtlas(fontId, pixelHeight(size * scale));
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            STBTTPackedchar glyph = glyphOf(atlas, text.charAt(i));
            width += glyph.xadvance();
        }
        return width / scale;
    }

    private static void initialize() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        roundedProgram = createProgram(VERTEX_SHADER, ROUNDED_FRAGMENT_SHADER);
        texturedProgram = createProgram(VERTEX_SHADER, TEXTURED_FRAGMENT_SHADER);
        texturedViewportUniform = glGetUniformLocation(texturedProgram, "viewport");
        texturedTexUniform = glGetUniformLocation(texturedProgram, "iconTex");
        viewportUniform = glGetUniformLocation(program, "viewport");
        colorUniform = glGetUniformLocation(program, "textColor");
        atlasUniform = glGetUniformLocation(program, "fontAtlas");
        roundedViewportUniform = glGetUniformLocation(roundedProgram, "viewport");
        roundedRectUniform = glGetUniformLocation(roundedProgram, "rect");
        roundedRadiusUniform = glGetUniformLocation(roundedProgram, "radius");
        roundedFillUniform = glGetUniformLocation(roundedProgram, "fillColor");
        roundedOutlineUniform = glGetUniformLocation(roundedProgram, "outlineColor");
        roundedOutlineWidthUniform = glGetUniformLocation(roundedProgram, "outlineWidth");
        roundedGlowUniform = glGetUniformLocation(roundedProgram, "glowColor");
        roundedGlowSizeUniform = glGetUniformLocation(roundedProgram, "glowSize");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);

        initialized = true;
    }

    private static ByteBuffer ensureFont(String fontId) {
        ByteBuffer data = FONT_DATA.get(fontId);
        if (data != null) return data;

        Fonts.FontEntry entry = Fonts.get(fontId);
        try {
            if (entry.systemPath() != null && Files.exists(entry.systemPath())) {
                data = MemoryUtil.memAlloc((int) Files.size(entry.systemPath()));
                data.put(Files.readAllBytes(entry.systemPath())).flip();
            } else {
                try (InputStream stream = RenderSystem.class.getResourceAsStream(
                        "/assets/neforclient/fonts/Inter.ttf")) {
                    if (stream == null) throw new IllegalStateException("Missing bundled font Inter.ttf");
                    byte[] bytes = stream.readAllBytes();
                    data = MemoryUtil.memAlloc(bytes.length);
                    data.put(bytes).flip();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load font: " + fontId, exception);
        }

        STBTTFontinfo info = STBTTFontinfo.malloc();
        if (!stbtt_InitFont(info, data)) {
            throw new IllegalStateException("Font could not be parsed: " + fontId);
        }
        FONT_DATA.put(fontId, data);
        FONT_INFO.put(fontId, info);
        return data;
    }

    private static int pixelHeight(float requested) {
        int rounded = Math.round(requested);
        return Math.clamp(rounded, MIN_PIXEL_HEIGHT, MAX_PIXEL_HEIGHT);
    }

    private static Atlas getAtlas(String fontId, int pixelHeight) {
        String key = fontId + ":" + pixelHeight;
        Atlas cached = ATLASES.get(key);
        if (cached != null) return cached;

        ByteBuffer fontData = ensureFont(fontId);
        STBTTFontinfo fontInfo = FONT_INFO.get(fontId);

        for (int size : ATLAS_SIZES) {
            ByteBuffer bitmap = MemoryUtil.memAlloc(size * size);
            STBTTPackedchar.Buffer latin = STBTTPackedchar.malloc(LATIN_COUNT);
            STBTTPackedchar.Buffer cyrillic = STBTTPackedchar.malloc(CYRILLIC_COUNT);
            boolean packed;
            STBTTPackContext context = STBTTPackContext.malloc();
            try {
                packed = stbtt_PackBegin(context, bitmap, size, size, 0, 1, MemoryUtil.NULL);
                if (packed) {
                    int oversample = pixelHeight > 90 ? 1 : pixelHeight > 60 ? 2 : OVERSAMPLE;
                    stbtt_PackSetOversampling(context, oversample, oversample);
                    packed = stbtt_PackFontRange(context, fontData, 0, pixelHeight, LATIN_FIRST, latin)
                            && stbtt_PackFontRange(context, fontData, 0, pixelHeight, CYRILLIC_FIRST, cyrillic);
                    stbtt_PackEnd(context);
                }
            } finally {
                context.free();
            }

            if (!packed) {
                latin.free();
                cyrillic.free();
                MemoryUtil.memFree(bitmap);
                continue;
            }

            int texture = uploadAtlas(bitmap, size);
            MemoryUtil.memFree(bitmap);

            float baseline;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ascent = stack.mallocInt(1);
                IntBuffer descent = stack.mallocInt(1);
                IntBuffer lineGap = stack.mallocInt(1);
                stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
                baseline = ascent.get(0) * stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);
            }

            Atlas atlas = new Atlas(fontId, texture, size, latin, cyrillic, baseline);
            ATLASES.put(key, atlas);
            LOGGER.info("Baked font atlas: {} {}px, {}x{}", fontId, pixelHeight, size, size);
            return atlas;
        }
        throw new IllegalStateException("Font did not fit into a 8192px atlas: "
                + fontId + " at " + pixelHeight + "px");
    }

    private static int uploadAtlas(ByteBuffer bitmap, int size) {
        int texture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SWAP_BYTES, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, size, size, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
        return texture;
    }

    private static STBTTPackedchar glyphOf(Atlas atlas, int codePoint) {
        if (codePoint >= LATIN_FIRST && codePoint < LATIN_FIRST + LATIN_COUNT) {
            return atlas.latin().get(codePoint - LATIN_FIRST);
        }
        if (codePoint >= CYRILLIC_FIRST && codePoint < CYRILLIC_FIRST + CYRILLIC_COUNT) {
            return atlas.cyrillic().get(codePoint - CYRILLIC_FIRST);
        }
        return atlas.latin().get('?' - LATIN_FIRST);
    }

    private static void drawTextPixels(String text, String fontId, float x, float y, float sizeInPixels,
                                       int width, int height,
                                       float red, float green, float blue, float alpha) {
        if (text.isEmpty()) return;
        Atlas atlas = getAtlas(fontId, pixelHeight(sizeInPixels));

        FloatBuffer vertices = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        FloatBuffer penX = BufferUtils.createFloatBuffer(1);
        FloatBuffer penY = BufferUtils.createFloatBuffer(1);
        try (STBTTAlignedQuad quad = STBTTAlignedQuad.malloc()) {
            penX.put(0, Math.round(x));
            penY.put(0, Math.round(y + atlas.baseline()));
            for (int i = 0; i < text.length(); i++) {
                int codePoint = text.charAt(i);
                STBTTPackedchar.Buffer range;
                int index;
                if (codePoint >= LATIN_FIRST && codePoint < LATIN_FIRST + LATIN_COUNT) {
                    range = atlas.latin();
                    index = codePoint - LATIN_FIRST;
                } else if (codePoint >= CYRILLIC_FIRST && codePoint < CYRILLIC_FIRST + CYRILLIC_COUNT) {
                    range = atlas.cyrillic();
                    index = codePoint - CYRILLIC_FIRST;
                } else {
                    range = atlas.latin();
                    index = '?' - LATIN_FIRST;
                }
                stbtt_GetPackedQuad(range, atlas.size(), atlas.size(), index, penX, penY, quad, true);
                putQuad(vertices, quad.x0(), quad.y0(), quad.x1(), quad.y1(),
                        quad.s0(), quad.t0(), quad.s1(), quad.t1());
            }
        }
        vertices.flip();

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousSampler = glGetInteger(GL_SAMPLER_BINDING);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);
        glUseProgram(program);
        glUniform2f(viewportUniform, width, height);
        glUniform4f(colorUniform, red, green, blue, alpha);
        glUniform1i(atlasUniform, 0);
        glBindTexture(GL_TEXTURE_2D, atlas.texture());
        glBindSampler(0, 0);
        setupOverlayState();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, vertices.limit() / 4);

        glBindVertexArray(previousVao);
        glBindSampler(0, previousSampler);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
        glActiveTexture(previousActiveTexture);
        if (blendWasEnabled) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (depthWasEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (scissorWasEnabled) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        glUseProgram(previousProgram);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius,
                                       int viewportWidth, int viewportHeight,
                                       float[] fillRgba, float[] outlineRgba, float outlineWidth,
                                       float[] glowRgba, float glowSize) {
        if (!initialized) initialize();

        float pad = glowSize > 0 ? glowSize * 3.0f : 0.0f;
        FloatBuffer vertices = BufferUtils.createFloatBuffer(6 * 4);
        putQuad(vertices, x - pad, y - pad, x + width + pad, y + height + pad, 0.0f, 0.0f, 1.0f, 1.0f);
        vertices.flip();

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);
        glUseProgram(roundedProgram);
        glUniform2f(roundedViewportUniform, viewportWidth, viewportHeight);
        glUniform4f(roundedRectUniform, x, y, width, height);
        glUniform1f(roundedRadiusUniform, radius);
        glUniform4f(roundedFillUniform, fillRgba[0], fillRgba[1], fillRgba[2], fillRgba[3]);
        glUniform4f(roundedOutlineUniform, outlineRgba[0], outlineRgba[1], outlineRgba[2], outlineRgba[3]);
        glUniform1f(roundedOutlineWidthUniform, outlineWidth);
        glUniform4f(roundedGlowUniform, glowRgba[0], glowRgba[1], glowRgba[2], glowRgba[3]);
        glUniform1f(roundedGlowSizeUniform, glowSize);
        setupOverlayState();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindVertexArray(previousVao);
        if (blendWasEnabled) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (depthWasEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (scissorWasEnabled) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        glUseProgram(previousProgram);
    }

    private static void setupOverlayState() {
        glColorMask(true, true, true, true);
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
    }

    private static void putQuad(FloatBuffer out, float x0, float y0, float x1, float y1,
                                float u0, float v0, float u1, float v1) {
        out.put(x0).put(y0).put(u0).put(v0);
        out.put(x1).put(y0).put(u1).put(v0);
        out.put(x1).put(y1).put(u1).put(v1);
        out.put(x0).put(y0).put(u0).put(v0);
        out.put(x1).put(y1).put(u1).put(v1);
        out.put(x0).put(y1).put(u0).put(v1);
    }

    public static void drawIcon(String name, double x, double y, double size) {
        drawTexture(name, x, y, size, size, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    public static int[] getTextureSize(String name) {
        iconTexture(name);
        return ICON_SIZES.getOrDefault(name, new int[]{1, 1});
    }

    /** Fills the target rect with the texture, cropping it like CSS background-size: cover. */
    public static void drawTextureCover(String name, double x, double y, double w, double h) {
        int[] size = getTextureSize(name);
        float texAspect = (float) size[0] / Math.max(1, size[1]);
        float rectAspect = (float) (w / Math.max(1.0, h));

        float u0 = 0.0f, v0 = 0.0f, u1 = 1.0f, v1 = 1.0f;
        if (texAspect > rectAspect) {
            float visible = rectAspect / texAspect;
            u0 = (1.0f - visible) / 2.0f;
            u1 = u0 + visible;
        } else {
            float visible = texAspect / rectAspect;
            v0 = (1.0f - visible) / 2.0f;
            v1 = v0 + visible;
        }
        drawTexture(name, x, y, w, h, u0, v0, u1, v1);
    }

    public static void drawTexture(String name, double x, double y,
                                   double w, double h,
                                   float u0, float v0, float u1, float v1) {
        int tex = iconTexture(name);
        if (tex == 0 || !initialized) return;

        MinecraftClient client = MinecraftClient.getInstance();
        float scale = (float) client.getWindow().getScaleFactor();

        FloatBuffer vertices = BufferUtils.createFloatBuffer(6 * 4);
        putQuad(vertices,
                (float) (x * scale), (float) (y * scale),
                (float) ((x + w) * scale), (float) ((y + h) * scale),
                u0, v0, u1, v1);
        vertices.flip();

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousSampler = glGetInteger(GL_SAMPLER_BINDING);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);

        glUseProgram(texturedProgram);
        glUniform2f(texturedViewportUniform,
                client.getWindow().getFramebufferWidth(),
                client.getWindow().getFramebufferHeight());
        glUniform1i(texturedTexUniform, 0);
        glBindTexture(GL_TEXTURE_2D, tex);
        glBindSampler(0, 0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindVertexArray(previousVao);
        glBindSampler(0, previousSampler);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
        glActiveTexture(previousActiveTexture);
        if (blendWasEnabled) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        glUseProgram(previousProgram);
    }

    private static int iconTexture(String name) {
        Integer cached = ICONS.get(name);
        if (cached != null) return cached;

        if (!initialized) initialize();

        int texture = 0;
        try (InputStream stream = RenderSystem.class.getResourceAsStream(
                "/assets/neforclient/icons/" + name + ".png")) {
            if (stream != null) {
                byte[] bytes = stream.readAllBytes();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer h = stack.mallocInt(1);
                    IntBuffer channels = stack.mallocInt(1);
                    ByteBuffer image = stbi_load_from_memory(stack.bytes(bytes), w, h, channels, 4);
                    if (image != null) {                        texture = glGenTextures();
                        glActiveTexture(GL_TEXTURE0);
                        int previous = glGetInteger(GL_TEXTURE_BINDING_2D);
                        glBindTexture(GL_TEXTURE_2D, texture);
                        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
                        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
                        glBindTexture(GL_TEXTURE_2D, previous);
                        ICON_SIZES.put(name, new int[]{w.get(0), h.get(0)});
                        stbi_image_free(image);
                        LOGGER.info("Icon {} loaded: {}x{}", name, w.get(0), h.get(0));
                    } else {
                        LOGGER.warn("Icon {} decode failed: {}", name, stbi_failure_reason());
                    }
                }
            } else {
                LOGGER.warn("Icon {} not found in resources", name);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to load icon {}", name, exception);
        }

        ICONS.put(name, texture);
        return texture;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertex = compile(GL_VERTEX_SHADER, vertexSource);
        int fragment = compile(GL_FRAGMENT_SHADER, fragmentSource);
        int result = glCreateProgram();
        glAttachShader(result, vertex);
        glAttachShader(result, fragment);
        glBindAttribLocation(result, 0, "position");
        glBindAttribLocation(result, 1, "uv");
        glLinkProgram(result);
        if (glGetProgrami(result, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("OpenGL shader link failed: " + glGetProgramInfoLog(result));
        }
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        return result;
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("OpenGL shader compile failed: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
