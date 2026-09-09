package exp.nefor.client.module;

import exp.nefor.client.config.AltsManager;
import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.event.api.EventBus;
import exp.nefor.client.event.api.EventHandler;
import exp.nefor.client.event.impl.ClientTickEvent;
import exp.nefor.client.gui.ClickGui;
import exp.nefor.client.hud.HudRenderer;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.impl.combat.KillAura;
import exp.nefor.client.module.impl.combat.NeuroAura;
import exp.nefor.client.module.impl.movement.AutoSprint;
import exp.nefor.client.module.impl.movement.NoFall;
import exp.nefor.client.module.impl.movement.WindHop;
import exp.nefor.client.module.impl.player.AntiAfk;
import exp.nefor.client.module.impl.player.AutoSell;
import exp.nefor.client.module.impl.player.FakePlayer;
import exp.nefor.client.module.impl.render.FullBright;
import exp.nefor.client.module.impl.render.Hud;
import exp.nefor.client.module.impl.render.NoRender;
import exp.nefor.client.module.impl.render.NoWeather;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.module.api.setting.Setting;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {

    private static final List<Module> MODULES = new ArrayList<>();

    private static final ModuleManager INSTANCE = new ModuleManager();

    private boolean guiWasPressed;

    private ModuleManager() {
    }

    public static void init() {
        if (!MODULES.isEmpty()) return;

        register(new AutoSell());
        register(new AntiAfk());
        register(new KillAura());
        register(new NeuroAura());
        register(new FakePlayer());
        register(new FullBright());
        register(new NoFall());
        register(new NoRender());
        register(new NoWeather());
        register(new Hud());
        register(new AutoSprint());
        register(new WindHop());

        EventBus.subscribe(INSTANCE);
        EventBus.subscribe(new HudRenderer());
        AltsManager.load();
        ConfigManager.load();
    }

    public static void register(Module module) {
        MODULES.add(module);
        EventBus.subscribe(module);
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module module : MODULES) {
            if (type.isInstance(module)) return (T) module;
        }
        return null;
    }

    public static Module get(String name) {
        for (Module module : MODULES) {
            if (module.getName().equals(name)) return module;
        }
        return null;
    }

    @EventHandler
    private void onTick(ClientTickEvent event) {
        MinecraftClient client = event.getClient();

        if (!client.isPaused()) {
            boolean guiPressed = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (guiPressed && !guiWasPressed && client.currentScreen == null) {
                client.setScreen(new ClickGui());
            }
            guiWasPressed = guiPressed;
        }

        boolean screenOpen = client.currentScreen != null;

        for (Module module : MODULES) {
            int key = module.getKeyCode();
            module.setPressed(key != GLFW.GLFW_KEY_UNKNOWN && !screenOpen
                    && InputUtil.isKeyPressed(client.getWindow(), key));
        }

        if (!screenOpen) {
            for (Module module : MODULES) {
                if (!module.isEnabled()) continue;
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof KeybindSetting keybind
                            && keybind.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN) {
                        keybind.onKeyPress(InputUtil.isKeyPressed(client.getWindow(), keybind.getKeyCode()));
                    }
                }
            }
        }

        for (Module module : MODULES) {
            if (module.isEnabled()) module.onTick();
        }
        // плавные ротации крутятся всегда
        exp.nefor.client.system.rotation.SmoothRotationManager.tick();
    }
}
