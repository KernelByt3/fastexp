package exp.nefor.client.mixin;

import exp.nefor.client.command.CommandManager;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void nefor$sendMessage(String chatText, boolean addToHistory, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(chatText.startsWith(".")){
            if(CommandManager.handle(chatText)){
                ci.cancel();
            }
        }
    }
}
