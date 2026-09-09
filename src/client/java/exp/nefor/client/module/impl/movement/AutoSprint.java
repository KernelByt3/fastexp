package exp.nefor.client.module.impl.movement;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class AutoSprint extends Module {

    private final BooleanSetting onlyWhenMoving = new BooleanSetting("Только при движении", true);

    public AutoSprint() {
        super("AutoSprint", "Автоматический спринт", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(onlyWhenMoving);
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;
        if (client.player.isSneaking()) return;
        if (onlyWhenMoving.getValue() && !client.player.input.hasForwardMovement()) return;
        if (!client.player.isSprinting()) {
            client.player.setSprinting(true);
        }
    }
}
