package exp.nefor.client.module.impl.movement;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {
    public NoFall(){ super("NoFall", "Не получаешь урон от падения", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN); }

    @Override public void onTick(){
        var p = MinecraftClient.getInstance().player;
        if(p==null || !isEnabled()) return;
        if(p.fallDistance > 2.5f && p.getVelocity().y < -0.1){
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }
}
