package exp.nefor.client.module.impl.player;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.ChoiceSetting;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.util.client.ClickUtil;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AntiAfk extends Module {

    private final BooleanSetting sway = new BooleanSetting("Покачивание", true);
    private final SliderSetting strength = new SliderSetting("Сила", 1, 10, 1, 3);
    private final ChoiceSetting mode = new ChoiceSetting("Режим", List.of("Плавно", "Рывки"), 0);
    private final KeybindSetting swayToggleBind = new KeybindSetting("Бинд покачивания", () -> sway.toggle());

    private int next = 0;

    public AntiAfk() {
        super("AntiAFK", "Качает головой, чтобы не выкинуло за АФК", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(sway, strength, mode, swayToggleBind);
    }

    @Override
    protected void onEnable() {
        next = ClickUtil.rand(40, 80);
    }

    @Override
    public void onTick() {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!sway.getValue()) return;

        if (--next <= 0) {
            next = ClickUtil.rand(40, 80);

            float baseYaw = client.player.getYaw();
            float basePitch = client.player.getPitch();
            float scale = (float) strength.getValue() / 10.0f;
            boolean smooth = mode.getValue().equals("Плавно");
            float time = System.currentTimeMillis() * (smooth ? 0.001f : 0.004f);

            float deltaYaw = (float) Math.sin(time) * ClickUtil.rand(20, 40) * scale;
            float deltaPitch = (float) Math.cos(time) * ClickUtil.rand(10, 20) * scale;

            client.player.setYaw(baseYaw + deltaYaw);
            client.player.setPitch(basePitch + deltaPitch);
        }
    }
}
