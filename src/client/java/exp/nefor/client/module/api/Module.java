package exp.nefor.client.module.api;

import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.gui.Bindable;
import exp.nefor.client.module.api.setting.Setting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module implements Bindable {

    private final String name;
    private final String description;
    private final Category category;
    private int keyCode;

    private boolean enabled;
    private boolean wasPressed;

    private final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category, int keyCode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = keyCode;
    }

    
    @Deprecated
    protected Module(String name, String description, String category, int keyCode) {
        this(name, description, Category.byLabel(category), keyCode);
    }

    public String getName() {
        return name;
    }

    public Category getCategoryEnum() {
        return category;
    }

    public String getCategory() {
        return category.getLabel();
    }

    @Override
    public String bindLabel() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        if (this.keyCode == keyCode) return;
        this.keyCode = keyCode;
        ConfigManager.save();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setPressed(boolean pressed) {
        if (pressed && !wasPressed) toggle();
        wasPressed = pressed;
    }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        RenderSystem.notification(getName() + (value ? " ВКЛ" : " ВЫКЛ"),
                value ? Color.GREEN : Color.RED);
        ConfigManager.save();
        if (value) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    protected void addSettings(Setting... values) {
        settings.addAll(List.of(values));
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }
}
