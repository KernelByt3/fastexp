package exp.nefor.client.gui;

import exp.nefor.client.config.AltsManager;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class AltsManagerScreen extends NeforScreen {

    private static final int W = 220;

    private final Screen parent;

    private String input = "";
    private boolean inputFocused = true;
    private String selectedAlt;
    private List<String> alts = List.of();
    private List<String> filtered = List.of();
    private double scroll = 0;
    private long blinkStart = System.currentTimeMillis();
    private final long openMs = System.currentTimeMillis();

    private float rowDy(int stagger) {
        float e = exp.nefor.client.util.AnimationUtil.openT(openMs, 80 + stagger * 35L, 240L);
        return (1f - e) * 10f;
    }

    public AltsManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
        refresh();
    }

    @Override public void removed() { AltsManager.save(); super.removed(); }

    private int cx() { return this.width / 2 - W / 2; }

    @Override
    public void renderNefor() {
        UiRender.background(this.width, this.height);

        int x = cx();
        int y = Math.max(20, this.height / 2 - 190);

        RenderSystem.drawText("alts", x + W / 2f - RenderSystem.textWidth("alts", 15f) / 2, y, 15f, 0xFFFFFFFF);
        String count = filtered.size() + "";
        RenderSystem.drawText(count, x + W - RenderSystem.textWidth(count, 10f), y + 3, 10f, 0xFF6E6E78);

        
        boolean fieldHover = UiRender.inBox(mouseX, mouseY, x, y + 26, W - 52, 22);
        UiRender.field(x, y + 26, W - 52, 22, inputFocused || fieldHover);
        String shown = input.isEmpty() ? "nick..." : input;
        boolean blink = (System.currentTimeMillis() - blinkStart) / 530 % 2 == 0;
        RenderSystem.drawText(shown + (inputFocused && blink ? "_" : ""), x + 8, y + 32, 11f,
                input.isEmpty() ? 0xFF6E6E78 : 0xFFEAEAF2);

        boolean randHover = UiRender.inBox(mouseX, mouseY, x + W - 46, y + 26, 46, 22);
        UiRender.field(x + W - 46, y + 26, 46, 22, randHover);
        RenderSystem.drawText("dice", x + W - 46 + 23 - RenderSystem.textWidth("dice", 10f) / 2, y + 32, 10f, 0xFF9A9AA5);

        
        boolean loginHover = UiRender.inBox(mouseX, mouseY, x, y + 52, W, 22);
        UiRender.button(x, y + 52, W, 22, "Login", loginHover, false);

        
        int listY = y + 82;
        int listH = Math.min(filtered.size() * 24 + 8, 192);
        UiRender.field(x, listY, W, listH, false);
        int rowH = 24;
        int start = (int) (scroll / rowH);
        int yOff = (int) (scroll % rowH);
        int drawn = 0;
        for (int i = start; i < filtered.size(); i++) {
            int rowY = listY + 4 + drawn * rowH - yOff + (int) rowDy(drawn);
            if (rowY + rowH < listY || rowY > listY + listH - 4) { drawn++; continue; }
            if (drawn * rowH > listH) break;
            String alt = filtered.get(i);
            boolean hovered = UiRender.inBox(mouseX, mouseY, x + 4, rowY, W - 8, rowH - 2);
            boolean selected = alt.equals(selectedAlt);
            if (selected || hovered) {
                UiRender.roundRect(x + 4, rowY, W - 8, rowH - 2, 4,
                        selected ? new float[]{1, 1, 1, 0.14f} : new float[]{1, 1, 1, 0.06f},
                        new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);
            }
            RenderSystem.drawText(alt, x + 12, rowY + 6, 11f, selected ? 0xFFFFFFFF : 0xFFB9B9C4);
            drawn++;
        }
        if (filtered.isEmpty()) {
            RenderSystem.drawText("empty", x + 12, listY + 10, 11f, 0xFF6E6E78);
        }

        
        int btnY = listY + listH + 8;
        int btnW = (W - 8) / 3;
        boolean b1h = UiRender.inBox(mouseX, mouseY, x, btnY, btnW, 20);
        boolean b2h = UiRender.inBox(mouseX, mouseY, x + btnW + 4, btnY, btnW, 20);
        boolean b3h = UiRender.inBox(mouseX, mouseY, x + (btnW + 4) * 2, btnY, btnW, 20);
        UiRender.button(x, btnY, btnW, 20, "Add", b1h, false);
        UiRender.button(x + btnW + 4, btnY, btnW, 20, "Del", b2h, b2h);
        UiRender.button(x + (btnW + 4) * 2, btnY, btnW, 20, "Back", b3h, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0 && click.button() != 1) return super.mouseClicked(click, doubled);
        int x = cx();
        int y = Math.max(20, this.height / 2 - 190);

        if (UiRender.inBox(mx, my, x, y + 26, W - 52, 22)) {
            inputFocused = true;
            blinkStart = System.currentTimeMillis();
            return true;
        }
        if (UiRender.inBox(mx, my, x + W - 46, y + 26, 46, 22)) {
            input = AltsManager.generateNick();
            inputFocused = true;
            selectedAlt = null;
            blinkStart = System.currentTimeMillis();
            return true;
        }
        if (UiRender.inBox(mx, my, x, y + 52, W, 22)) {
            String name = !input.isBlank() ? input : selectedAlt;
            if (name != null && !name.isBlank() && AltsManager.login(name)) close();
            else inputFocused = true;
            return true;
        }

        int listY = y + 82;
        int listH = Math.min(filtered.size() * 24 + 8, 192);
        int btnY = listY + listH + 8;
        int btnW = (W - 8) / 3;
        if (UiRender.inBox(mx, my, x, btnY, btnW, 20)) {
            String name = !input.isBlank() ? input.trim() : selectedAlt;
            if (name != null && !name.isEmpty()) { AltsManager.add(name); refresh(); inputFocused = true; }
            return true;
        }
        if (UiRender.inBox(mx, my, x + btnW + 4, btnY, btnW, 20)) {
            if (selectedAlt != null) { AltsManager.remove(selectedAlt); selectedAlt = null; input = ""; refresh(); }
            return true;
        }
        if (UiRender.inBox(mx, my, x + (btnW + 4) * 2, btnY, btnW, 20)) { close(); return true; }

        if (my >= listY && my < listY + listH && mx >= x + 4 && mx <= x + W - 4) {
            int rowH = 24;
            int start = (int) (scroll / rowH);
            int yOff = (int) (scroll % rowH);
            int idx = start + (int) (my - listY - 4 + yOff) / rowH;
            if (idx >= 0 && idx < filtered.size()) {
                
                int staggerDy = (int) rowDy(idx - start);
                int idxAdj = start + (int) (my - listY - 4 + yOff - staggerDy) / rowH;
                if (idxAdj < 0 || idxAdj >= filtered.size()) return true;
                String alt = filtered.get(idxAdj);
                if (doubled) {
                    if (AltsManager.login(alt)) close();
                    return true;
                }
                selectedAlt = alt;
                input = alt;
                inputFocused = true;
                if (click.button() == 1) this.client.keyboard.setClipboard(alt);
                return true;
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hx, double vy) {
        int listY = Math.max(20, this.height / 2 - 190) + 82;
        int listH = Math.min(filtered.size() * 24 + 8, 192);
        if (y >= listY && y < listY + listH) {
            scroll = Math.max(0, Math.min(scroll - vy * 22, Math.max(0, filtered.size() * 24 - listH)));
            return true;
        }
        return super.mouseScrolled(x, y, hx, vy);
    }

    private void refresh() {
        alts = AltsManager.getAlts();
        applyFilter();
    }

    private void applyFilter() {
        if (input.isEmpty()) filtered = List.copyOf(alts);
        else filtered = alts.stream().filter(s -> s.toLowerCase().contains(input.toLowerCase())).collect(Collectors.toList());
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() * 24 - Math.min(filtered.size() * 24 + 8, 192))));
    }

    @Override
    public boolean charTyped(CharInput ci) {
        if (inputFocused && ci.isValidChar() && input.length() < 16) {
            int cp = ci.codepoint();
            if (Character.isLetterOrDigit(cp) || cp == '_') {
                input += ci.asString();
                blinkStart = System.currentTimeMillis();
                applyFilter();
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
            if (k == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
                applyFilter();
                blinkStart = System.currentTimeMillis();
                return true;
            }
            if (k == GLFW.GLFW_KEY_ENTER) {
                String name = !input.isBlank() ? input.trim() : selectedAlt;
                if (name != null && !name.isEmpty() && AltsManager.login(name)) close();
                return true;
            }
            if (k == GLFW.GLFW_KEY_TAB) { inputFocused = false; return true; }
        }
        return super.keyPressed(ki);
    }

    @Override
    public void close() {
        AltsManager.save();
        if (this.client != null) this.client.setScreen(parent);
    }
}
