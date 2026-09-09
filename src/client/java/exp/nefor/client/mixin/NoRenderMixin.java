package exp.nefor.client.mixin;

import exp.nefor.client.module.ModuleManager;
import exp.nefor.client.module.impl.render.NoRender;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class NoRenderMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void nefor$fire(MatrixStack matrices, CallbackInfo ci){
        var m = ModuleManager.get(NoRender.class);
        if(m!=null && m.isEnabled() && m.fire.getValue()) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void nefor$scoreboard(CallbackInfo ci){
        var m = ModuleManager.get(NoRender.class);
        if(m!=null && m.isEnabled() && m.scoreboard.getValue()) ci.cancel();
    }

    @Inject(method = "renderBossBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void nefor$bossbar(CallbackInfo ci){
        var m = ModuleManager.get(NoRender.class);
        if(m!=null && m.isEnabled() && m.bossbar.getValue()) ci.cancel();
    }
}
