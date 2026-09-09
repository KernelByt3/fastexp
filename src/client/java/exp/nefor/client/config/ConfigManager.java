package exp.nefor.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.ModuleManager;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.ChoiceSetting;
import exp.nefor.client.module.api.setting.Setting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.render.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("nefor/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loading;

    public static Path configsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("configs");
    }
    private static Path path() {
        return configsDir().resolve(current + ".json");
    }
    private static String current = "nefor";

    public static String getCurrent() { return current; }

    public static synchronized void load() {
        loading = true;
        try {
            Path path = path();
            if (!Files.exists(path)) {
                // миграция со старого пути
                Path old = FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("config").resolve("nefor.json");
                if (Files.exists(old) && current.equals("nefor")) {
                    Files.createDirectories(path.getParent());
                    Files.copy(old, path);
                } else return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            if (root.has("font")) {
                RenderSystem.setActiveFont(root.get("font").getAsString());
            }

            if (!root.has("modules")) return;

            for (JsonElement element : root.getAsJsonArray("modules")) {
                JsonObject moduleJson = element.getAsJsonObject();
                Module module = ModuleManager.get(moduleJson.get("name").getAsString());
                if (module == null) continue;

                if (moduleJson.has("bind")) {
                    module.setKeyCode(moduleJson.get("bind").getAsInt());
                }
                if (moduleJson.has("enabled") && moduleJson.get("enabled").getAsBoolean()) {
                    module.setEnabled(true);
                }
                if (!moduleJson.has("settings")) continue;

                JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                for (Setting setting : module.getSettings()) {
                    applySetting(settingsJson, setting);
                }
            }
            LOGGER.info("Config loaded from {}", path);
        } catch (Exception exception) {
            LOGGER.warn("Failed to load config", exception);
        } finally {
            loading = false;
        }
    }

    private static void applySetting(JsonObject settingsJson, Setting setting) {
        String name = setting.getName();
        if (!settingsJson.has(name)) return;

        JsonElement value = settingsJson.get(name);
        if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.setValue(value.getAsBoolean());
        } else if (setting instanceof SliderSetting sliderSetting) {
            sliderSetting.setValue(value.getAsDouble());
        } else if (setting instanceof ChoiceSetting choiceSetting) {
            choiceSetting.setIndex(value.getAsInt());
        } else if (setting instanceof exp.nefor.client.module.api.setting.KeybindSetting keybind) {
            keybind.setKeyCode(value.getAsInt());
        }
    }

    public static synchronized void save() { save(current); }
    public static synchronized void save(String name) {
        if (loading) return;
        String prev = current;
        current = name;
        try {
            Files.createDirectories(path().getParent());
            JsonObject root = new JsonObject();
            root.addProperty("font", RenderSystem.getActiveFont());
            var modules = new com.google.gson.JsonArray();
            for (Module module : ModuleManager.getModules()) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("name", module.getName());
                moduleJson.addProperty("description", module.getDescription());
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("bind", module.getKeyCode());
                JsonObject settingsJson = new JsonObject();
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting booleanSetting) settingsJson.addProperty(setting.getName(), booleanSetting.getValue());
                    else if (setting instanceof SliderSetting sliderSetting) settingsJson.addProperty(setting.getName(), sliderSetting.getValue());
                    else if (setting instanceof ChoiceSetting choiceSetting) settingsJson.addProperty(setting.getName(), choiceSetting.getIndex());
                    else if (setting instanceof exp.nefor.client.module.api.setting.KeybindSetting keybind) settingsJson.addProperty(setting.getName(), keybind.getKeyCode());
                }
                moduleJson.add("settings", settingsJson);
                modules.add(moduleJson);
            }
            root.add("modules", modules);
            Files.writeString(path(), GSON.toJson(root));
            LOGGER.info("Saved config {}", name);
        } catch (Exception e){ LOGGER.warn("Failed to save config", e); }
        current = prev;
        if(!name.equals(prev)) current = name;
    }
    public static synchronized void load(String name){
        current = name;
        load();
    }
    public static java.util.List<String> listConfigs(){
        try{ Files.createDirectories(configsDir()); return Files.list(configsDir()).filter(p->p.toString().endsWith(".json")).map(p->p.getFileName().toString().replace(".json","")).sorted().toList(); }catch(Exception e){ return java.util.List.of();}
    }
    public static boolean delete(String name){
        try{ return Files.deleteIfExists(configsDir().resolve(name+".json")); }catch(Exception e){ return false; }
    }
}
