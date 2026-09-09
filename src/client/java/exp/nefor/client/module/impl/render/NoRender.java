package exp.nefor.client.module.impl.render;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class NoRender extends Module {
    public final BooleanSetting fire = new BooleanSetting("Огонь", true);
    public final BooleanSetting scoreboard = new BooleanSetting("Скорборд", true);
    public final BooleanSetting bossbar = new BooleanSetting("Боссбар", false);
    public final BooleanSetting pumpkin = new BooleanSetting("Тыква", true);
    public final BooleanSetting hurtCam = new BooleanSetting("Тряска", true);
    public final BooleanSetting fog = new BooleanSetting("Туман", false);
    public final BooleanSetting rain = new BooleanSetting("Дождь/частицы огня", true);

    public NoRender(){
        super("NoRender", "Убирает лишнее", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(fire, scoreboard, bossbar, pumpkin, hurtCam, fog, rain);
    }
}
