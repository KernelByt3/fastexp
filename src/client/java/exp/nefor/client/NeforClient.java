package exp.nefor.client;

import exp.nefor.client.gui.UiRender;
import exp.nefor.client.mixin.ConnectScreenAccessor;
import exp.nefor.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;

public class NeforClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		exp.nefor.client.system.FriendManager.load();
		exp.nefor.client.system.MacroManager.init();
		exp.nefor.client.config.ConfigManager.load();

		// клик по нашей кнопке Cancel на ConnectScreen (ваниль погашена)
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof ConnectScreen) {
				ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
					if (click.button() == 0 && UiRender.inBox(click.x(), click.y(), s.width / 2 - 90, s.height / 2 + 44, 180, 22)) {
						ConnectScreenAccessor acc = (ConnectScreenAccessor) s;
						acc.nefor$setConnectingCancelled(true);
						try {
							if (acc.nefor$connection() != null) acc.nefor$connection().disconnect(ConnectScreen.ABORTED_TEXT);
						} catch (Exception ignored) {
						}
						client.setScreen(acc.nefor$parent());
						return false;
					}
					return true;
				});
			}
		});
	}
}
