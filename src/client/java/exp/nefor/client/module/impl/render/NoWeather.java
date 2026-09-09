package exp.nefor.client.module.impl.render;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class NoWeather extends Module {
    public NoWeather(){ super("NoWeather", "Убирает дождь", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onTick(){
        var mc = MinecraftClient.getInstance();
        if(mc.world!=null) mc.world.setRainGradient(0); mc.world.setThunderGradient(0);
    }
}
