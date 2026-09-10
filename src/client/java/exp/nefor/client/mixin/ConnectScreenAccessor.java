package exp.nefor.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConnectScreen.class)
public interface ConnectScreenAccessor {

    @Accessor("parent")
    Screen nefor$parent();

    @Accessor("connection")
    ClientConnection nefor$connection();

    @Accessor("connectingCancelled")
    void nefor$setConnectingCancelled(boolean value);

    @Accessor("status")
    Text nefor$status();
}
