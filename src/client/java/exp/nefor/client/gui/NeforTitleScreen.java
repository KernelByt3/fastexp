package exp.nefor.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class NeforTitleScreen extends Screen implements CustomRenderedScreen {

    private record Button(String label, int x, int y, int w, int h, boolean danger, Runnable action) {}

    private double mouseX;
    private double mouseY;
    private final long openMs = System.currentTimeMillis();

    
    private float btnDy(int i) {
        float e = exp.nefor.client.util.AnimationUtil.openT(openMs, 60 + i * 45L, 260L);
        return (1f - e) * 16f;
    }

    public NeforTitleScreen() {
        super(Text.literal("NeforClient"));
    }

    private List<Button> buttons() {
        List<Button> list = new ArrayList<>();
        int w = 180;
        int centerX = this.width / 2 - w / 2;
        int y = this.height / 2 - 44;
        int gap = 26;
        list.add(new Button("Singleplayer", centerX, y, w, 22, false, () -> this.client.setScreen(new SelectWorldScreen(this))));
        list.add(new Button("Multiplayer", centerX, y + gap, w, 22, false, () -> this.client.setScreen(new MultiplayerScreen(this))));
        list.add(new Button("Alts", centerX, y + gap * 2, w, 22, false, () -> this.client.setScreen(new AltsManagerScreen(this))));
        list.add(new Button("Neural Training", centerX, y + gap * 3, w, 22, false, () -> this.client.setScreen(new NeuralTrainingScreen())));
        list.add(new Button("Settings", centerX, y + gap * 4, (w - 4) / 2, 22, false, () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        list.add(new Button("Quit", centerX + (w + 4) / 2, y + gap * 4, (w - 4) / 2, 22, true, this.client::scheduleStop));
        return list;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        
    }

    @Override
    public void nefor$renderOverlay() {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        UiRender.background(this.width, this.height);

        float titleE = exp.nefor.client.util.AnimationUtil.openT(openMs, 0L, 300L);
        float titleSize = 26.0f;
        float titleWidth = exp.nefor.client.render.RenderSystem.textWidth("nefor", titleSize);
        float titleX = this.width / 2.0f - titleWidth / 2.0f;
        float titleY = this.height / 2.0f - 100.0f - (1f - titleE) * 12f;
        exp.nefor.client.render.RenderSystem.drawText("nefor", titleX, titleY, titleSize, 0xFFFFFFFF);

        String sub = "fabric 1.21.11";
        exp.nefor.client.render.RenderSystem.drawText(sub,
                this.width / 2.0f - exp.nefor.client.render.RenderSystem.textWidth(sub, 11.0f) / 2.0f,
                titleY + 30.0f, 11.0f, 0xFF9A9AA5);

        List<Button> btns = buttons();
        for (int i = 0; i < btns.size(); i++) {
            Button button = btns.get(i);
            float dy = btnDy(i);
            boolean hovered = UiRender.inBox(mouseX, mouseY, button.x(), button.y() + dy, button.w(), button.h());
            UiRender.button(button.x(), button.y() + dy, button.w(), button.h(), button.label(), hovered, button.danger());
        }

        String hint = "Right Shift — menu";
        exp.nefor.client.render.RenderSystem.drawText(hint,
                this.width / 2.0f - exp.nefor.client.render.RenderSystem.textWidth(hint, 10.0f) / 2.0f,
                this.height - 20.0f, 10.0f, 0xFF6E6E78);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        List<Button> btns = buttons();
        for (int i = 0; i < btns.size(); i++) {
            Button button = btns.get(i);
            if (UiRender.inBox(click.x(), click.y(), button.x(), button.y() + btnDy(i), button.w(), button.h())) {
                button.action().run();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
