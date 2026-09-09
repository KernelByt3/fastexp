package exp.nefor.client.module.impl.render;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class FullBright extends Module {
    private double prevGamma;

    public FullBright(){
        super("FullBright", "Максимальная яркость", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    protected void onEnable(){
        var mc = MinecraftClient.getInstance();
        prevGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(15.0);
    }

    @Override
    protected void onDisable(){
        MinecraftClient.getInstance().options.getGamma().setValue(prevGamma);
    }
}
