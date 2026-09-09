package exp.nefor.client.util.player.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChatUtil {

    public static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (message.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(message);
        }
    }

    public static void sendMessageToClient(String message, boolean overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), overlay);
        }
    }
}
