package exp.nefor.client.module.impl.render;

import exp.nefor.client.event.api.EventHandler;
import exp.nefor.client.event.impl.HudRenderEvent;
import exp.nefor.client.gui.UiRender;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.render.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 3D ESP + nametags через наш рендер: боксы и ники поверх мира.
 */
public class ESP extends Module {

    private final BooleanSetting players = new BooleanSetting("Игроки", true);
    private final BooleanSetting mobs = new BooleanSetting("Мобы", true);
    private final BooleanSetting items = new BooleanSetting("Предметы", false);
    private final BooleanSetting boxes = new BooleanSetting("Боксы", true);
    private final BooleanSetting nametags = new BooleanSetting("Ники", true);
    private final BooleanSetting health = new BooleanSetting("ХП", true);
    private final SliderSetting range = new SliderSetting("Дистанция", 10, 120, 5, 60);

    public ESP() {
        super("ESP", "Боксы и ники сквозь стены", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(players, mobs, items, boxes, nametags, health, range);
    }

    @EventHandler
    public void onHud(HudRenderEvent event) {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.gameRenderer == null) return;
        if (client.currentScreen != null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();
        int fov = 70;
        try {
            fov = client.options.getFov().getValue();
        } catch (Exception ignored) {
        }

        float sw = client.getWindow().getScaledWidth();
        float sh = client.getWindow().getScaledHeight();
        double aspect = sw / Math.max(1, sh);
        double tanH = Math.tan(Math.toRadians(fov / 2.0));
        double tanW = tanH * aspect;

        double yawR = Math.toRadians(camYaw);
        double pitchR = Math.toRadians(camPitch);
        double sy = Math.sin(yawR), cy = Math.cos(yawR);
        double sp = Math.sin(pitchR), cp = Math.cos(pitchR);
        // базис камеры: forward/right/up
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, ry = 0, rz = -sy;
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double maxDist = range.getValue();
        for (Entity e : client.world.getEntities()) {
            if (e == client.player || e.isRemoved()) continue;
            if (e.distanceTo(client.player) > maxDist) continue;
            String tag = tagFor(e);
            if (tag == null) continue;

            Box box = e.getBoundingBox().expand(0.05);
            float[] rect = project(box, camPos,
                    fx, fy, fz, rx, ry, rz, ux, uy, uz,
                    sw, sh, tanW, tanH);
            if (rect == null) continue;

            boolean friend = false;
            try {
                if (e instanceof PlayerEntity pe) {
                    friend = exp.nefor.client.system.FriendManager.isFriend(pe.getName().getString());
                }
            } catch (Exception ignored) {
            }
            float[] col = friend ? new float[]{0.3f, 1f, 0.5f, 0.9f} : new float[]{1f, 1f, 1f, 0.85f};

            if (boxes.getValue()) {
                float x0 = rect[0], y0 = rect[1], x1 = rect[2], y1 = rect[3];
                float t = 1.2f;
                float[] noLine = new float[]{0, 0, 0, 0};
                UiRender.roundRect(x0 - t, y0 - t, (x1 - x0) + t * 2, t, 0, col, noLine, 0, noLine, 0);
                UiRender.roundRect(x0 - t, y1, (x1 - x0) + t * 2, t, 0, col, noLine, 0, noLine, 0);
                UiRender.roundRect(x0 - t, y0, t, (y1 - y0), 0, col, noLine, 0, noLine, 0);
                UiRender.roundRect(x1, y0, t, (y1 - y0) + t, 0, col, noLine, 0, noLine, 0);
            }
            if (nametags.getValue()) {
                String hp = "";
                if (health.getValue() && e instanceof LivingEntity le) {
                    hp = " " + (int) Math.ceil(le.getHealth());
                }
                String text = tag + hp;
                float tw = RenderSystem.textWidth(text, 10f);
                float tx = (rect[0] + rect[2]) / 2f - tw / 2f;
                float ty = rect[1] - 13f;
                UiRender.roundRect(tx - 3, ty - 2, tw + 6, 13, 3,
                        new float[]{0.02f, 0.02f, 0.04f, 0.75f},
                        new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);
                RenderSystem.drawText(text, tx, ty, 10f, 0xFFFFFFFF);
            }
        }
    }

    private String tagFor(Entity e) {
        if (e instanceof PlayerEntity p) {
            if (!players.getValue()) return null;
            if (p.isCreative() || p.isSpectator()) return null;
            return p.getName().getString();
        }
        if (e instanceof HostileEntity) {
            if (!mobs.getValue()) return null;
            return e.getType().getName().getString();
        }
        if (e instanceof ItemEntity ie) {
            if (!items.getValue()) return null;
            return ie.getStack().getName().getString();
        }
        return null;
    }

    /** Проекция бокса в экран. null если полностью за камерой. */
    private static float[] project(Box box, Vec3d cam,
                                   double fx, double fy, double fz,
                                   double rx, double ry, double rz,
                                   double ux, double uy, double uz,
                                   float sw, float sh, double tanW, double tanH) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean any = false;
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) for (double y : ys) for (double z : zs) {
            double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
            double cx = dx * rx + dy * ry + dz * rz;
            double cyy = dx * ux + dy * uy + dz * uz;
            double cz = dx * fx + dy * fy + dz * fz;
            if (cz < 0.1) continue;
            double sx = sw / 2.0 + (cx / cz) / tanW * (sw / 2.0);
            double syy = sh / 2.0 - (cyy / cz) / tanH * (sh / 2.0);
            minX = Math.min(minX, sx);
            minY = Math.min(minY, syy);
            maxX = Math.max(maxX, sx);
            maxY = Math.max(maxY, syy);
            any = true;
        }
        if (!any) return null;
        float m = 80f;
        if (maxX < -m || minX > sw + m || maxY < -m || minY > sh + m) return null;
        return new float[]{(float) minX, (float) minY, (float) maxX, (float) maxY};
    }

    @Override
    public void onTick() {
    }
}
