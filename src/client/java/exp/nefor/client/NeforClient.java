package exp.nefor.client;

import exp.nefor.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public class NeforClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		exp.nefor.client.system.FriendManager.load();
		exp.nefor.client.system.MacroManager.init();
		exp.nefor.client.config.ConfigManager.load();
	}
}
