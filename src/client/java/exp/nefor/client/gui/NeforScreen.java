package exp.nefor.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class NeforScreen extends Screen implements CustomRenderedScreen {

    protected double mouseX;
    protected double mouseY;

    protected NeforScreen(Text title) {
        super(title);
    }

    @Override
    public final void nefor$renderOverlay() {
        mouseX = scaledMouseX();
        mouseY = scaledMouseY();
        renderNefor();
    }

    protected abstract void renderNefor();

    protected double scaledMouseX() {
        float scale = this.client.getWindow().getScaleFactor();
        return this.client.mouse.getX() / scale;
    }

    protected double scaledMouseY() {
        float scale = this.client.getWindow().getScaleFactor();
        return this.client.mouse.getY() / scale;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
