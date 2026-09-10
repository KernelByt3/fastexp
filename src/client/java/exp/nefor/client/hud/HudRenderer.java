package exp.nefor.client.hud;

import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.event.api.EventHandler;
import exp.nefor.client.event.impl.HudRenderEvent;
import exp.nefor.client.gui.UiRender;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.ModuleManager;
import exp.nefor.client.module.impl.render.Hud;
import exp.nefor.client.module.impl.combat.KillAura;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HudRenderer {

    public static final float GRID = 5.0f;

    public static final String[] ELEMENTS = {"watermark", "keybinds", "cooldowns", "targethud"};

    
    public static final Map<String, float[]> lastRects = new java.util.HashMap<>();

    
    public static boolean editMode = false;
    public static String dragging = null;
    public static float dragOffX = 0;
    public static float dragOffY = 0;
    private static boolean wasDown = false;

    @EventHandler
    public void onRender(HudRenderEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Hud hud = ModuleManager.get(Hud.class);
        if (hud == null || !hud.isEnabled()) return;

        if (editMode || client.currentScreen instanceof ChatScreen) {
            long handle = client.getWindow().getHandle();
            boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            pollDrag(client, (float) client.mouse.getX(), (float) client.mouse.getY(), down);
            renderEditor(client, hud);
            return;
        }

        Map<String, float[]> rects = computeRects(client, hud);
        lastRects.clear();
        lastRects.putAll(rects);

        if (hud.watermark.getValue()) drawWatermark(rects.get("watermark"));
        if (hud.keybinds.getValue()) drawKeybinds(client, rects.get("keybinds"));
        if (hud.cooldowns.getValue()) drawCooldowns(client, rects.get("cooldowns"));
        if (hud.targetHud.getValue()) drawTargetHud(client, rects.get("targethud"));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Map<String, float[]> computeRects(MinecraftClient client, Hud hud) {
        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();
        Map<String, float[]> map = new java.util.HashMap<>();

        
        {
            float size = 21.0f, padX = 9.0f, padY = 5.0f, dot = 8.0f, gap = 6.0f;
            String base = "Nefor";
            String tag = "client";
            String info = "";
            try{
                int fps = client.getCurrentFps();
                info = fps+" fps";
                if(client.getNetworkHandler()!=null && client.player!=null){
                    var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                    if(e!=null) info += " | "+e.getLatency()+" ms";
                }
            }catch(Exception ignored){}
            info += " | b" + exp.nefor.client.NeforClient.BUILD;
            float w = padX + dot + gap + RenderSystem.textWidth(base, size) + 3 + RenderSystem.textWidth(tag, 9f) + 10 + RenderSystem.textWidth(info, 8f) + padX + 6;
            w = Math.max(w, 148f);
            float h = size + padY * 2;
            float[] p = hud.getPos("watermark");
            map.put("watermark", new float[]{
                    clamp((float) p[0], 0, Math.max(0, sw - w)),
                    clamp((float) p[1], 0, Math.max(0, sh - h)),
                    w, h});
        }

        
        {
            List<Module> enabled = new ArrayList<>();
            for (Module m : ModuleManager.getModules()) if(m.isEnabled() && m.getKeyCode()!=GLFW.GLFW_KEY_UNKNOWN) enabled.add(m);
            float padX = 10f;
            float panelW = 150f;
            for(Module m: enabled){
                float lineW = RenderSystem.textWidth(m.getName(), 11.5f) + 16 + RenderSystem.textWidth(keyLabel(m.getKeyCode()), 9f) + 12 + padX*2;
                panelW = Math.max(panelW, lineW);
            }
            panelW = Math.min(panelW, sw * 0.45f);
            float panelH = Math.max(30f, enabled.size()*18f + 22);
            float[] p = hud.getPos("keybinds");
            map.put("keybinds", new float[]{
                    clamp((float) p[0], 0, Math.max(0, sw - panelW)),
                    clamp((float) p[1], 0, Math.max(0, sh - panelH)),
                    panelW, panelH});
        }

        
        {
            List<String[]> rows = collectCooldowns(client);
            float textSize = 12.0f, rowH = 22.0f, padX = 9.0f, gap = 6.0f;
            float maxW = 0.0f;
            for (String[] row : rows) {
                float lineW = RenderSystem.textWidth(row[0], textSize)
                        + gap + RenderSystem.textWidth(row[1], textSize);
                maxW = Math.max(maxW, lineW);
            }
            float panelW = Math.max(130.0f, maxW + padX * 2);
            float panelH = Math.max(30.0f, rows.size() * rowH + 8);
            float[] p = hud.getPos("cooldowns");
            map.put("cooldowns", new float[]{
                    clamp((float) p[0], 0, Math.max(0, sw - panelW)),
                    clamp((float) p[1], 0, Math.max(0, sh - panelH)),
                    panelW, panelH});
        }

        
        {
            float w = 172.0f, h = 54.0f;
            float[] p = hud.getPos("targethud");
            map.put("targethud", new float[]{
                    clamp((float) p[0], 0, Math.max(0, sw - w)),
                    clamp((float) p[1], 0, Math.max(0, sh - h)),
                    w, h});
        }

        return map;
    }

    private static String keyLabel(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "[ ]";
        if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_5) {
            return "[M" + (keyCode + 1) + "]";
        }
        String name = switch (keyCode) {
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACK";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            default -> null;
        };
        if (name == null) {
            String glfw = GLFW.glfwGetKeyName(keyCode, 0);
            name = glfw == null ? "KEY" + keyCode : glfw;
        }
        return "[" + name.toUpperCase() + "]";
    }

    private static float[] toFloats(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                ((color >> 24) & 0xFF) / 255.0f
        };
    }

    private static void drawBar(float x, float y, float w, float h, float fraction, int color) {
        
        UiRender.roundRect(x, y, w, h, h / 2,
                new float[]{0.04f, 0.03f, 0.08f, 0.92f},
                new float[]{1,1,1,0.06f},1,
                new float[]{0,0,0,0},0);
        float f = Math.max(0,Math.min(1,fraction));
        float fw = w * f;
        if (fw > 0.8f) {
            
            float glow = 0f;
            if (f >= 0.99f) glow = 0.35f + 0.15f * (float)Math.sin(System.currentTimeMillis()*0.01);
            UiRender.roundRect(x, y, fw, h, h / 2,
                    toFloats(color), new float[]{0, 0, 0, 0}, 0,
                    new float[]{toFloats(color)[0], toFloats(color)[1], toFloats(color)[2], glow}, glow>0?5:0);
            
            UiRender.roundRect(x, y, fw, h*0.45f, h/2, new float[]{1,1,1,0.14f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        } else if (fw > 0.4f) {
            UiRender.roundRect(x, y, fw, h, h / 2,
                    toFloats(color), new float[]{0, 0, 0, 0}, 0,
                    new float[]{0,0,0,0},0);
        }
    }

    private static String formatPct(float v) {
        return (int) (v * 100) + "%";
    }

    
    private static float lastFps = 60;
    private static void drawWatermark(float[] r) {
        float x = r[0], y = r[1], w = r[2], h = r[3];
        float size = 21.0f, padX = 9.0f, padY = 5.0f, dot = 8.0f, gap = 6.0f;

        UiRender.roundRect(x, y, w, h, 8,
                new float[]{0.06f,0.06f,0.09f,0.94f},
                new float[]{1,1,1,0.08f}, 1,
                new float[]{0,0,0,0}, 0);

        float dotX = x + padX;
        float dotY = y + (h - dot) / 2;
        UiRender.roundRect(dotX, dotY, dot, dot, dot/2,
                new float[]{1f,1f,1f,0.9f},
                new float[]{0,0,0,0},0,
                new float[]{0,0,0,0},0);

        RenderSystem.drawText("Nefor", x + padX + dot + gap, y + padY + 1, size, 0xFFFFFFFF);
        
        String tag = "client";
        RenderSystem.drawText(tag, x + padX + dot + gap + RenderSystem.textWidth("Nefor", size)+3, y + padY + 8, 9f, 0xFF9A9AA6);
        
        try{
            var mc = MinecraftClient.getInstance();
            int fps = mc.getCurrentFps();
            lastFps = lastFps*0.92f + fps*0.08f;
            String info = (int)lastFps+" fps";
            if(mc.getNetworkHandler()!=null && mc.player!=null){
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if(e!=null) info += " | "+e.getLatency()+" ms";
            }
            info += " | b" + exp.nefor.client.NeforClient.BUILD;
            RenderSystem.drawText(info, x+padX+dot+gap+ RenderSystem.textWidth("Nefor", size)+ RenderSystem.textWidth(tag,9f)+10, y+padY+8, 8f, 0xFF6E6E7A);
        }catch(Exception ignored){}
    }

    
    private static void drawKeybinds(MinecraftClient client, float[] r) {
        float x = r[0], y = r[1], w = r[2], h = r[3];
        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.getModules()) if(m.isEnabled() && m.getKeyCode()!=GLFW.GLFW_KEY_UNKNOWN) enabled.add(m);
        if(enabled.isEmpty()) return;
        enabled.sort(Comparator.comparing(Module::getName));

        UiRender.roundRect(x, y, w, h, 8,
                new float[]{0.06f,0.06f,0.09f,0.94f},
                new float[]{1,1,1,0.07f}, 1,
                new float[]{0,0,0,0}, 0);
        
        UiRender.roundRect(x, y, w, 18, 8, new float[]{1,1,1,0.04f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        RenderSystem.drawText("KEYBINDS", x+10, y+5, 8f, 0xFF9A9AA6);
        float cy = y + 20;
        for(Module m: enabled){
            String name = m.getName();
            String key = keyLabel(m.getKeyCode());
            RenderSystem.drawText(name, x+10, cy+2, 11.5f, 0xFFEAEAF2);
            float kw = RenderSystem.textWidth(key, 9f)+8;
            UiRender.roundRect(x+w-10-kw, cy-1, kw, 14, 4, new float[]{1,1,1,0.08f}, new float[]{1,1,1,0.10f},1, new float[]{0,0,0,0},0);
            RenderSystem.drawText(key, x+w-10-kw+4, cy+2, 9f, 0xFFB9B9C4);
            cy += 18;
        }
    }

    
    private static void drawCooldowns(MinecraftClient client, float[] r) {
        float x = r[0], y = r[1], w = r[2], h = r[3];
        List<String[]> rows = collectCooldowns(client);
        if (rows.isEmpty()) return;

        float textSize = 12.0f, rowH = 22.0f, padX = 9.0f;

        UiRender.roundRect(x, y, w, h, 7,
                new float[]{0.06f,0.06f,0.09f,0.92f},
                new float[]{1, 1, 1, 0.08f}, 1,
                new float[]{0,0,0,0}, 0);

        float cy = y + 4.0f;
        for (String[] row : rows) {
            float v = Float.parseFloat(row[1].replace("%", "")) / 100.0f;
            RenderSystem.drawText(row[0], x + padX, cy + 2, textSize, 0xFFE6E6F0);
            float pctW = RenderSystem.textWidth(row[1], textSize);
            RenderSystem.drawText(row[1], x + w - padX - pctW, cy + 2, textSize, 0xFFB0B0C0);

            int color = v >= 0.99f ? 0xFF4BE37A : v > 0.5f ? 0xFFE3C53B : 0xFFE5484B;
            drawBar(x + padX, cy + textSize + 3, w - padX * 2, 5.0f, v, color);
            cy += rowH;
        }
    }

    private static List<String[]> collectCooldowns(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        List<String[]> rows = new ArrayList<>();
        if (player == null) return rows;

        Map<String, Float> items = new LinkedHashMap<>();
        collectCooldown(player, player.getMainHandStack(), items);
        collectCooldown(player, player.getOffHandStack(), items);
        for (int i = 0; i < player.getInventory().size(); i++) {
            collectCooldown(player, player.getInventory().getStack(i), items);
        }
        for (Map.Entry<String, Float> e : items.entrySet()) {
            if (e.getValue() > 0.001f) rows.add(new String[]{e.getKey(), formatPct(e.getValue())});
        }
        return rows;
    }

    private static void collectCooldown(ClientPlayerEntity player, ItemStack stack, Map<String, Float> map) {
        if (stack == null || stack.isEmpty()) return;
        float cp = player.getItemCooldownManager().getCooldownProgress(stack, 0.0f);
        if (cp > 0.0f) {
            String name = stack.getName().getString();
            map.put(name, Math.max(map.getOrDefault(name, 0.0f), cp));
        }
    }

    
    private static float prevHp = 20f;
    private static int lastTargetId = -1;
    private static float pop = 0f;
    private static float slide = 0f;
    private static float flash = 0f;

    private static void drawHead(PlayerEntity p, float x, float y, float size) {
        try {
            var client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() == null) return;
            var entry = client.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (entry == null) return;
            var tex = client.getTextureManager().getTexture(entry.getSkinTextures().texturePath());
            int glId = ((net.minecraft.client.texture.AbstractTexture) tex).getGlId();
            if (glId == 0) return;
            RenderSystem.drawGlTexture(glId, x, y, size, size, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f);
            RenderSystem.drawGlTexture(glId, x, y, size, size, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f);
        } catch (Exception ignored) {
        }
    }

    private static void drawTargetHud(MinecraftClient client, float[] r) {
        float x = r[0], y = r[1], w = r[2], h = r[3];
        KillAura ka = ModuleManager.get(KillAura.class);
        LivingEntity target = ka == null ? null : ka.getTarget();
        boolean has = target instanceof PlayerEntity && target.isAlive() && !target.isRemoved();
        slide += ((has ? 1 : 0) - slide) * 0.18f;
        if (slide < 0.02f) {
            lastTargetId = -1;
            return;
        }

        int tid = has ? target.getId() : -1;
        if (tid != lastTargetId) {
            lastTargetId = tid;
            pop = 0f;
        }
        pop += (1f - pop) * 0.22f;

        x = x - (1 - slide) * 18;
        float pw = w * (0.82f + 0.18f * pop);
        float ph = h * (0.88f + 0.12f * pop);
        float px = x + (w - pw);
        float py = y + (h - ph) / 2f;

        UiRender.roundRect(px, py, pw, ph, 9,
                new float[]{0.055f, 0.055f, 0.085f, 0.96f},
                new float[]{1, 1, 1, 0.08f}, 1,
                new float[]{0, 0, 0, 0}, 0);

        PlayerEntity p = has ? (PlayerEntity) target : null;
        float av = 36f;
        float avX = px + 9;
        float avY = py + (ph - av) / 2;
        // рамка аватара
        UiRender.roundRect(avX - 1, avY - 1, av + 2, av + 2, 7,
                new float[]{0.03f, 0.03f, 0.06f, 0.95f},
                new float[]{1, 1, 1, 0.12f}, 1,
                new float[]{0, 0, 0, 0}, 0);
        // подложка
        UiRender.roundRect(avX, avY, av, av, 6,
                new float[]{0.13f, 0.12f, 0.18f, 1f},
                new float[]{0, 0, 0, 0}, 0,
                new float[]{0, 0, 0, 0}, 0);

        if (p != null) {
            // голова из скина (лицо + шляпа)
            drawHead(p, avX, avY, av);

            String name = p.getName().getString();
            if (name.length() > 14) name = name.substring(0, 14);
            RenderSystem.drawText(name, px + av + 16, py + 8, 13f, 0xFFFFFFFF);

            float hp = target.getHealth();
            float max = Math.max(1, target.getMaxHealth());
            if (hp < prevHp - 0.5f) flash = 1f;
            flash += (0f - flash) * 0.15f;
            prevHp += (hp - prevHp) * 0.18f;
            float frac = Math.max(0, Math.min(1, prevHp / max));

            String sub = String.format("%.1f HP", Math.max(0, hp));
            RenderSystem.drawText(sub, px + av + 16, py + 22, 9f, 0xFF9A9AA6);

            float barX = px + av + 16;
            float barW = pw - (av + 16) - 10;
            float barY = py + 34;
            int hpColor = frac > 0.55f ? 0xFF4BE37A : frac > 0.28f ? 0xFFE3C53B : 0xFFE5484B;
            drawBar(barX, barY, barW, 6f, frac, hpColor);
            if (flash > 0.03f) {
                UiRender.roundRect(px, py, pw, ph, 9,
                        new float[]{0, 0, 0, 0}, new float[]{1, 1, 1, 0.35f * flash}, 1,
                        new float[]{0, 0, 0, 0}, 0);
            }
        } else if (slide > 0.5f && lastTargetId != -1) {
            RenderSystem.drawText("...", px + av + 16, py + 12, 13f, 0xFF6E6E78);
        }
    }

    
    public static void pollDrag(MinecraftClient client, float mxRaw, float myRaw, boolean down) {
        Hud hud = ModuleManager.get(Hud.class);
        if (hud == null) { wasDown = down; return; }
        float scale = (float) client.getWindow().getScaleFactor();
        float mx = mxRaw / scale;
        float my = myRaw / scale;

        Map<String, float[]> rects = computeRects(client, hud);
        lastRects.clear();
        lastRects.putAll(rects);

        if (down && !wasDown) {
            
            for (int i = ELEMENTS.length-1; i>=0; i--) {
                String name = ELEMENTS[i];
                float[] r = lastRects.get(name);
                
                boolean visible = switch(name){
                    case "watermark" -> hud.watermark.getValue();
                    case "keybinds" -> hud.keybinds.getValue();
                    case "cooldowns" -> hud.cooldowns.getValue();
                    case "targethud" -> hud.targetHud.getValue();
                    default -> true;
                };
                if(!visible) continue;
                if (r != null && mx >= r[0]-2 && mx <= r[0] + r[2]+2 && my >= r[1]-2 && my <= r[1] + r[3]+2) {
                    dragging = name;
                    dragOffX = mx - r[0];
                    dragOffY = my - r[1];
                    break;
                }
            }
        }
        if (dragging != null && down) {
            float nx = Math.round((mx - dragOffX) / GRID) * GRID;
            float ny = Math.round((my - dragOffY) / GRID) * GRID;
            float sw = client.getWindow().getScaledWidth();
            float sh = client.getWindow().getScaledHeight();
            float[] cur = lastRects.get(dragging);
            float w = cur==null? 100: cur[2];
            float h = cur==null? 20: cur[3];
            nx = Math.max(0, Math.min(nx, sw - w));
            ny = Math.max(0, Math.min(ny, sh - h));
            hud.setPos(dragging, nx, ny);
        }
        if (!down && wasDown && dragging != null) {
            ConfigManager.save();
            dragging = null;
        }
        wasDown = down;
    }

    private static void renderEditor(MinecraftClient client, Hud hud) {
        Map<String, float[]> rects = computeRects(client, hud);
        lastRects.clear();
        lastRects.putAll(rects);

        
        if (hud.watermark.getValue()) drawWatermark(rects.get("watermark"));
        if (hud.keybinds.getValue()) drawKeybinds(client, rects.get("keybinds"));
        if (hud.cooldowns.getValue()) drawCooldowns(client, rects.get("cooldowns"));
        if (hud.targetHud.getValue()) drawTargetHud(client, rects.get("targethud"));

        RenderSystem.drawText("HUD editor (H off): hold LMB and drag on 5px grid", 8, 8, 13, 0xFFB9B9C4);

        for (String name : ELEMENTS) {
            float[] r = rects.get(name);
            if (r == null) continue;
            boolean isDragging = name.equals(dragging);
            UiRender.roundRect(r[0], r[1], r[2], r[3], 7,
                    isDragging
                            ? new float[]{1f, 1f, 1f, 0.14f}
                            : new float[]{0.07f, 0.06f, 0.12f, 0.55f},
                    new float[]{1, 1, 1, 0.25f}, 1.5f,
                    new float[]{0, 0, 0, 0}, 0);

            String label = switch (name) {
                case "watermark" -> "Watermark";
                case "keybinds" -> "Keybinds";
                case "cooldowns" -> "Cooldowns";
                case "targethud" -> "TargetHud";
                default -> name;
            };
            float tw = RenderSystem.textWidth(label, 12);
            RenderSystem.drawText(label, r[0] + (r[2] - tw) / 2, r[1] + (r[3] - 12) / 2, 12, 0xFFFFFFFF);
        }
    }
}
