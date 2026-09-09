package exp.nefor.client.module.api.setting;

import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.gui.Bindable;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting implements Bindable {

    private final Runnable action;

    private int keyCode = GLFW.GLFW_KEY_UNKNOWN;
    private boolean wasPressed;

    public KeybindSetting(String name, Runnable action) {
        super(name);
        this.action = action;
    }

    public int getKeyCode() {
        return keyCode;
    }

    @Override
    public String bindLabel() {
        return getName();
    }

    public void setKeyCode(int keyCode) {
        if (this.keyCode == keyCode) return;
        this.keyCode = keyCode;
        changed();
    }

    public void onKeyPress(boolean pressed) {
        boolean edge = pressed && !wasPressed;
        wasPressed = pressed;
        if (edge && action != null) action.run();
    }
}
