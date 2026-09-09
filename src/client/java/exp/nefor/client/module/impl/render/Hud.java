package exp.nefor.client.module.impl.render;

import exp.nefor.client.gui.HudEditorScreen;
import exp.nefor.client.hud.HudRenderer;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class Hud extends Module {

    public final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    public final SliderSetting watermarkX = new SliderSetting("Watermark X", 0, 3000, 5, 6);
    public final SliderSetting watermarkY = new SliderSetting("Watermark Y", 0, 2000, 5, 6);

    public final BooleanSetting keybinds = new BooleanSetting("Keybinds", true);
    public final SliderSetting keybindsX = new SliderSetting("Keybinds X", 0, 3000, 5, 1200);
    public final SliderSetting keybindsY = new SliderSetting("Keybinds Y", 0, 2000, 5, 6);

    public final BooleanSetting cooldowns = new BooleanSetting("Cooldowns", true);
    public final SliderSetting cooldownsX = new SliderSetting("Cooldowns X", 0, 3000, 5, 1200);
    public final SliderSetting cooldownsY = new SliderSetting("Cooldowns Y", 0, 2000, 5, 400);

    public final BooleanSetting targetHud = new BooleanSetting("TargetHud", true);
    public final SliderSetting targetHudX = new SliderSetting("TargetHud X", 0, 3000, 5, 820);
    public final SliderSetting targetHudY = new SliderSetting("TargetHud Y", 0, 2000, 5, 30);

    public final KeybindSetting editorBind = new KeybindSetting("Редактор HUD", this::toggleEditor);

    public Hud() {
        super("Hud", "Элементы интерфейса и их редактирование", Category.INTERFACE, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(
                watermark, watermarkX, watermarkY,
                keybinds, keybindsX, keybindsY,
                cooldowns, cooldownsX, cooldownsY,
                targetHud, targetHudX, targetHudY,
                editorBind
        );
        editorBind.setKeyCode(GLFW.GLFW_KEY_H);
    }

    private void toggleEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!HudRenderer.editMode) {
            HudRenderer.editMode = true;
            if (!(client.currentScreen instanceof HudEditorScreen)) {
                client.setScreen(new HudEditorScreen());
            }
            RenderSystem.notification("Редактор HUD включён — тащи элементы ЛКМ", 0xFF9B6BFF);
        } else {
            HudRenderer.editMode = false;
            HudRenderer.dragging = null;
            if (client.currentScreen instanceof HudEditorScreen) {
                client.setScreen(null);
            }
            RenderSystem.notification("Редактор HUD выключен", 0xFF9B6BFF);
        }
    }

    public float[] getPos(String name) {
        return switch (name) {
            case "watermark" -> new float[]{(float) watermarkX.getValue(), (float) watermarkY.getValue()};
            case "keybinds" -> new float[]{(float) keybindsX.getValue(), (float) keybindsY.getValue()};
            case "cooldowns" -> new float[]{(float) cooldownsX.getValue(), (float) cooldownsY.getValue()};
            case "targethud" -> new float[]{(float) targetHudX.getValue(), (float) targetHudY.getValue()};
            default -> new float[]{0, 0};
        };
    }

    public void setPos(String name, float x, float y) {
        switch (name) {
            case "watermark" -> {
                watermarkX.setRaw(x);
                watermarkY.setRaw(y);
            }
            case "keybinds" -> {
                keybindsX.setRaw(x);
                keybindsY.setRaw(y);
            }
            case "cooldowns" -> {
                cooldownsX.setRaw(x);
                cooldownsY.setRaw(y);
            }
            case "targethud" -> {
                targetHudX.setRaw(x);
                targetHudY.setRaw(y);
            }
        }
    }
}
