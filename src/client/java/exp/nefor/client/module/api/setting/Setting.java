package exp.nefor.client.module.api.setting;

import exp.nefor.client.config.ConfigManager;

public abstract class Setting {

    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected void changed() {
        ConfigManager.save();
    }
}
