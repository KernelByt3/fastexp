package exp.nefor.client.module.api.setting;

public class BooleanSetting extends Setting {

    private boolean value;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        if (this.value == value) return;
        this.value = value;
        changed();
    }

    public void toggle() {
        setValue(!value);
    }
}
