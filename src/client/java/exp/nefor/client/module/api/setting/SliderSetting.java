package exp.nefor.client.module.api.setting;

public class SliderSetting extends Setting {

    private final double min;
    private final double max;
    private final double step;

    private double value;

    public SliderSetting(String name, double min, double max, double step, double defaultValue) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = clamp(defaultValue);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        value = clamp(value);
        if (this.value == value) return;
        this.value = value;
        changed();
    }

    
    public void setRaw(double value) {
        this.value = clamp(value);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    private double clamp(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) {
            v = Math.round(v / step) * step;
            v = Math.round(v * 1000.0) / 1000.0;
        }
        return Math.max(min, Math.min(max, v));
    }
}
