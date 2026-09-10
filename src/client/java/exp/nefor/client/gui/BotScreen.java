package exp.nefor.client.gui;

import exp.nefor.client.module.impl.player.Bot;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Окно ботов: поле ника, сервер, список. ПКМ по боту — вселение/возврат
 * (трансляция вида + управление), ЛКМ — выбор.
 */
public class BotScreen extends NeforScreen {

    private static final int W = 260;

    private String nickInput = "";
    private boolean inputFocused = true;
    private long blinkStart = System.currentTimeMillis();
    private final long openMs = System.currentTimeMillis();

    public BotScreen() {
        super(Text.literal("Bots"));
    }

    private Bot bot() { return Bot.get(); }
    private int cx() { return this.width / 2 - W / 2; }
    private int top() { return Math.max(16, this.height / 2 - 200); }

    private float inDy(int stagger) {
        float e = exp.nefor.client.util.AnimationUtil.openT(openMs, 60 + stagger * 40L, 240L);
        return (1f - e) * 10f;
    }

    @Override
    public void renderNefor() {
        Bot b = bot();
        UiRender.background(this.width, this.height);
        int x = cx();
        int y = top();

        RenderSystem.drawText("bots", x + W / 2f - RenderSystem.textWidth("bots", 15f) / 2, y + inDy(0), 15f, 0xFFFFFFFF);
        String srv = b == null ? "?" : Bot.currentServer();
        RenderSystem.drawText(srv, x + W / 2f - RenderSystem.textWidth(srv, 9f) / 2, y + 20 + inDy(0), 9f, 0xFF6E6E78);

        // поле ника + добавить
        boolean fh = UiRender.inBox(mouseX, mouseY, x, y + 36, W - 64, 22);
        UiRender.field(x, y + 36, W - 64, 22, inputFocused || fh);
        String shown = nickInput.isEmpty() ? "nick..." : nickInput;
        boolean blink = (System.currentTimeMillis() - blinkStart) / 530 % 2 == 0;
        RenderSystem.drawText(shown + (inputFocused && blink ? "_" : ""), x + 8, y + 42, 11f,
                nickInput.isEmpty() ? 0xFF6E6E78 : 0xFFEAEAF2);
        boolean addH = UiRender.inBox(mouseX, mouseY, x + W - 58, y + 36, 58, 22);
        UiRender.button(x + W - 58, y + 36, 58, 22, "Add", addH, false);

        // список
        int listY = y + 66;
        int rowH = 26;
        java.util.List<Bot.Entry> bots = b == null ? java.util.List.of() : b.getBots();
        int listH = Math.max(34, Math.min(bots.size() * (rowH + 4) + 8, 200));
        UiRender.field(x, listY, W, listH, false);
        for (int i = 0; i < bots.size(); i++) {
            Bot.Entry en = bots.get(i);
            int rowY = listY + 4 + i * (rowH + 4) + (int) inDy(i + 1);
            if (rowY > listY + listH - 10) break;
            boolean sel = en.equals(b.getSelected());
            boolean live = en.equals(b.getPossessed());
            boolean hov = UiRender.inBox(mouseX, mouseY, x + 4, rowY, W - 8, rowH);
            if (sel || hov || live) {
                UiRender.roundRect(x + 4, rowY, W - 8, rowH, 4,
                        live ? new float[]{1, 1, 1, 0.16f} : selectedFill(sel, hov),
                        new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);
            }
            String label = en.nick + (live ? "  LIVE" : "");
            RenderSystem.drawText(label, x + 12, rowY + 8, 11f, live ? 0xFFFFFFFF : sel ? 0xFFFFFFFF : 0xFFB9B9C4);
            String mode = en.follow ? "follow" : "stay";
            RenderSystem.drawText(mode, x + W - 12 - RenderSystem.textWidth(mode, 9f), rowY + 9, 9f, 0xFF6E6E78);
        }
        if (bots.isEmpty()) {
            RenderSystem.drawText("empty — add nick above", x + 12, listY + 11, 10f, 0xFF6E6E78);
        }

        // низ: режим / ко мне / закрыть
        int btnY = listY + listH + 8;
        int btnW = (W - 8) / 3;
        boolean h1 = UiRender.inBox(mouseX, mouseY, x, btnY, btnW, 20);
        boolean h2 = UiRender.inBox(mouseX, mouseY, x + btnW + 4, btnY, btnW, 20);
        boolean h3 = UiRender.inBox(mouseX, mouseY, x + (btnW + 4) * 2, btnY, btnW, 20);
        UiRender.button(x, btnY, btnW, 20, "Follow", h1, false);
        UiRender.button(x + btnW + 4, btnY, btnW, 20, "To me", h2, false);
        UiRender.button(x + (btnW + 4) * 2, btnY, btnW, 20, "Del", h3, h3);

        RenderSystem.drawText("LMB select | RMB possess | ESC close",
                x + W / 2f - RenderSystem.textWidth("LMB select | RMB possess | ESC close", 8f) / 2,
                btnY + 26, 8f, 0xFF6E6E78);
    }

    private static float[] selectedFill(boolean sel, boolean hov) {
        if (sel) return new float[]{1, 1, 1, 0.12f};
        if (hov) return new float[]{1, 1, 1, 0.06f};
        return new float[]{0, 0, 0, 0};
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Bot b = bot();
        if (b == null) { close(); return true; }
        double mx = click.x(), my = click.y();
        int x = cx();
        int y = top();

        if (UiRender.inBox(mx, my, x, y + 36, W - 64, 22)) {
            inputFocused = true;
            blinkStart = System.currentTimeMillis();
            return true;
        }
        if (UiRender.inBox(mx, my, x + W - 58, y + 36, 58, 22)) {
            if (click.button() == 0) {
                Bot.Entry en = b.spawnBot(nickInput.isBlank() ? null : nickInput.trim());
                if (en != null) nickInput = "";
            }
            return true;
        }

        int listY = y + 66;
        int rowH = 26;
        java.util.List<Bot.Entry> bots = b.getBots();
        int listH = Math.max(34, Math.min(bots.size() * (rowH + 4) + 8, 200));
        for (int i = 0; i < bots.size(); i++) {
            int rowY = listY + 4 + i * (rowH + 4);
            if (UiRender.inBox(mx, my, x + 4, rowY - 6, W - 8, rowH + 12)) {
                Bot.Entry en = bots.get(i);
                if (click.button() == 1) b.possess(en); // ПКМ — вселение/возврат
                else b.setSelected(en);
                return true;
            }
        }

        int btnY = listY + listH + 8;
        int btnW = (W - 8) / 3;
        Bot.Entry sel = b.getSelected();
        if (UiRender.inBox(mx, my, x, btnY, btnW, 20)) {
            if (sel != null) { sel.follow = !sel.follow; }
            return true;
        }
        if (UiRender.inBox(mx, my, x + btnW + 4, btnY, btnW, 20)) {
            b.bringToMe(sel); // бот идёт на основу
            return true;
        }
        if (UiRender.inBox(mx, my, x + (btnW + 4) * 2, btnY, btnW, 20)) {
            b.removeBot(sel);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharInput ci) {
        if (inputFocused && ci.isValidChar() && nickInput.length() < 16) {
            int cp = ci.codepoint();
            if (Character.isLetterOrDigit(cp) || cp == '_' || cp == '-') {
                nickInput += ci.asString();
                blinkStart = System.currentTimeMillis();
                return true;
            }
        }
        return super.charTyped(ci);
    }

    @Override
    public boolean keyPressed(KeyInput ki) {
        int k = ki.key();
        if (k == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (inputFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !nickInput.isEmpty()) {
                nickInput = nickInput.substring(0, nickInput.length() - 1);
                blinkStart = System.currentTimeMillis();
                return true;
            }
            if (k == GLFW.GLFW_KEY_ENTER) {
                Bot b = bot();
                if (b != null) {
                    Bot.Entry en = b.spawnBot(nickInput.isBlank() ? null : nickInput.trim());
                    if (en != null) nickInput = "";
                }
                return true;
            }
        }
        return super.keyPressed(ki);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(null);
    }
}
