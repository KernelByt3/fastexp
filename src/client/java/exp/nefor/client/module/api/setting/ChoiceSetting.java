package exp.nefor.client.module.api.setting;

import java.util.List;

public class ChoiceSetting extends Setting {

    private final List<String> options;
    private int index;

    public ChoiceSetting(String name, List<String> options, int defaultIndex) {
        super(name);
        this.options = List.copyOf(options);
        this.index = Math.clamp(defaultIndex, 0, this.options.size() - 1);
    }

    public List<String> getOptions() {
        return options;
    }

    public String getValue() {
        return options.get(index);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        int clamped = Math.floorMod(index, options.size());
        if (this.index == clamped) return;
        this.index = clamped;
        changed();
    }

    public void cycle(int direction) {
        setIndex(index + direction);
    }
}
