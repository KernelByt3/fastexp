package exp.nefor.client.gui;

import exp.nefor.client.render.RenderSystem;
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

    public NeforTitleScreen() {
        super(Text.literal("NeforClient"));
    }

    private List<Button> buttons() {
        List<Button> list = new ArrayList<>();
        int centerX = this.width / 2 - 100;
        int y = this.height / 2 - 60;
        list.add(new Button("Одиночная игра", centerX, y, 200, 24, false, () -> this.client.setScreen(new SelectWorldScreen(this))));
        list.add(new Button("Сетевая игра", centerX, y + 28, 200, 24, false, () -> this.client.setScreen(new MultiplayerScreen(this))));
        list.add(new Button("Alt Manager", centerX, y + 56, 200, 24, false, () -> this.client.setScreen(new AltsManagerScreen(this))));
        list.add(new Button("Нейро Тренировка  [AI]", centerX, y + 84, 200, 24, false, () -> this.client.setScreen(new NeuralTrainingScreen())));
        list.add(new Button("Настройки", centerX, y + 112, 98, 24, false, () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        list.add(new Button("Выход", centerX + 102, y + 112, 98, 24, true, this.client::scheduleStop));
        return list;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Drawn from the end-of-frame hook instead: see nefor$renderOverlay.
    }

    @Override
    public void nefor$renderOverlay() {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        SakuraBackground.draw(System.currentTimeMillis());
        UiRender.dimScreen(this.width, this.height, 0.35f);

        for (Button button : buttons()) {
            boolean hovered = UiRender.inBox(mouseX, mouseY, button.x(), button.y(), button.w(), button.h());
            UiRender.button(button.x(), button.y(), button.w(), button.h(), button.label(), hovered, button.danger());
        }

        float titleSize = 30.0f;
        float titleWidth = RenderSystem.textWidth("NEFOR", titleSize)
                + 8.0f + RenderSystem.textWidth("CLIENT", titleSize);
        float titleX = this.width / 2.0f - titleWidth / 2.0f;
        float titleY = this.height / 4.0f - 20.0f;
        RenderSystem.drawText("NEFOR", titleX, titleY, titleSize, 0xFFC9A6FF);
        RenderSystem.drawText("CLIENT", titleX + RenderSystem.textWidth("NEFOR", titleSize) + 8.0f,
                titleY, titleSize, 0xFFFFFFFF);

        String owner = "Owner: Java_1v  •  NeuroAura epochs: " + exp.nefor.client.system.neural.NeuroModel.get().epochsTrained + "  dataset: " + exp.nefor.client.system.neural.NeuroDataset.size();
        RenderSystem.drawText(owner, 10.0f, this.height - 16.0f, 10.0f, 0xFFC9A6FF);

        String hint = "Right Shift — ClickGui  |  Neuro Training — обучи ауру под себя";
        RenderSystem.drawText(hint, this.width / 2.0f - RenderSystem.textWidth(hint, 10.0f) / 2.0f,
                this.height - 24.0f, 10.0f, 0xFFFFFFFF);
        // неоновая полоска
        float pulse = 0.6f + 0.4f * (float)Math.sin(System.currentTimeMillis()*0.004);
        UiRender.roundRect(this.width/2f - 80, this.height/4f + 28, 160, 2, 1, new float[]{0.36f,0.49f,1f,0.7f*pulse}, new float[]{0,0,0,0},0, new float[]{0.36f,0.49f,1f,0.3f},4);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (Button button : buttons()) {
            if (UiRender.inBox(click.x(), click.y(), button.x(), button.y(), button.w(), button.h())) {
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

    protected double scaledMouseX() {
        float scale = (float) this.client.getWindow().getScaleFactor();
        return this.client.mouse.getX() / scale;
    }

    protected double scaledMouseY() {
        float scale = (float) this.client.getWindow().getScaleFactor();
        return this.client.mouse.getY() / scale;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
