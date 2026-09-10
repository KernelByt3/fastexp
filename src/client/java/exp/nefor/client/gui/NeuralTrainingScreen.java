package exp.nefor.client.gui;

import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.system.neural.NeuroDataset;
import exp.nefor.client.system.neural.NeuroModel;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

import java.util.Random;

public class NeuralTrainingScreen extends NeforScreen {
    private final Random rnd = new Random();
    private float targetX, targetY;
    private float targetR = 14f;
    private long spawnTime;
    private int hits = 0;
    private int misses = 0;
    private int quest = 0;
    private final int totalQuests = 60;
    private String status = "Hover the dot";
    private long lastHitMs = 0;

    public NeuralTrainingScreen() { super(Text.literal("Neuro Training")); }

    @Override
    public void init() {
        super.init();
        spawnTarget();
    }

    private void spawnTarget() {
        targetX = 50 + rnd.nextFloat() * (width - 100);
        targetY = 60 + rnd.nextFloat() * (height - 120);
        targetR = 14 + rnd.nextFloat() * 6;
        spawnTime = System.currentTimeMillis();
        quest++;
    }

    @Override
    public void renderNefor() {
        UiRender.background(width, height);

        String title = "neuro training";
        RenderSystem.drawText(title, width / 2f - RenderSystem.textWidth(title, 15f) / 2, 16, 15f, 0xFFFFFFFF);

        String count = Math.min(quest, totalQuests) + " / " + totalQuests;
        RenderSystem.drawText(count, width / 2f - RenderSystem.textWidth(count, 10f) / 2, 34, 10f, 0xFF9A9AA5);

        float prog = (float) hits / totalQuests;
        UiRender.roundRect(width / 2f - 90, 48, 180, 3, 1, new float[]{1, 1, 1, 0.12f}, new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);
        UiRender.roundRect(width / 2f - 90, 48, 180 * prog, 3, 1, new float[]{1, 1, 1, 0.9f}, new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);

        if (quest > totalQuests) {
            String done = "Done — hits: " + hits + " / " + totalQuests + "   miss: " + misses;
            RenderSystem.drawText(done, width / 2f - RenderSystem.textWidth(done, 11f) / 2, height / 2f - 8, 11f, 0xFFFFFFFF);
            var model = NeuroModel.get();
            String loss = String.format("dataset: %d   epochs: %d   loss: %.5f", NeuroDataset.size(), model.epochsTrained, model.lastLoss);
            RenderSystem.drawText(loss, width / 2f - RenderSystem.textWidth(loss, 9f) / 2, height / 2f + 12, 9f, 0xFF9A9AA5);
            String keys = "ESC — back   |   R — restart";
            RenderSystem.drawText(keys, width / 2f - RenderSystem.textWidth(keys, 9f) / 2, height / 2f + 28, 9f, 0xFF6E6E78);
            return;
        }

        double dxH = mouseX - targetX, dyH = mouseY - targetY;
        boolean hovering = Math.hypot(dxH, dyH) <= targetR;
        if (hovering && System.currentTimeMillis() - spawnTime > 80) {
            long react = System.currentTimeMillis() - spawnTime;
            float dist = (float) Math.hypot(dxH, dyH);
            NeuroDataset.add(new NeuroDataset.Sample((float) dxH * 0.08f, (float) dyH * 0.08f, dist * 0.04f, dist / (react + 1f), react, true));
            hits++;
            status = "Hit " + react + "ms";
            lastHitMs = System.currentTimeMillis();
            if (NeuroDataset.size() % 3 == 0) NeuroModel.get().train(5);
            if (hits >= totalQuests) {
                quest = totalQuests + 1;
                NeuroDataset.forceSave();
            }
            else spawnTarget();
        }

        
        float pop = exp.nefor.client.util.AnimationUtil.easeOutCubic(
                Math.min(1f, (System.currentTimeMillis() - spawnTime) / 160f));
        float rr = Math.max(2f, targetR * (0.3f + 0.7f * pop));
        UiRender.roundRect(targetX - rr, targetY - rr, rr * 2, rr * 2, 999,                hovering ? new float[]{1, 1, 1, 1f} : new float[]{1, 1, 1, 0.25f},
                new float[]{1, 1, 1, hovering ? 0.9f : 0.3f}, 1,
                new float[]{0, 0, 0, 0}, 0);
        UiRender.roundRect(targetX - 2, targetY - 2, 4, 4, 2,
                new float[]{1, 1, 1, 0.9f}, new float[]{0, 0, 0, 0}, 0, new float[]{0, 0, 0, 0}, 0);

        RenderSystem.drawText(status, width / 2f - RenderSystem.textWidth(status, 10f) / 2, height - 20, 10f, 0xFF9A9AA5);
        if (lastHitMs > 0 && System.currentTimeMillis() - lastHitMs < 500) {
            RenderSystem.drawText("HIT", targetX + targetR + 8, targetY - 6, 11f, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (quest > totalQuests) return super.mouseClicked(click, doubled);
        double dx = click.x() - targetX, dy = click.y() - targetY;
        if (Math.hypot(dx, dy) > targetR) {
            misses++;
            status = "Miss — aim closer";
            NeuroDataset.add(new NeuroDataset.Sample((float) dx * 0.08f, (float) dy * 0.08f, (float) Math.hypot(dx, dy) * 0.04f, 0.5f, System.currentTimeMillis() - spawnTime, false));
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput ki) {
        if (quest > totalQuests) {
            if (ki.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                NeuroModel.get().train(20);
                status = "Trained 20 epochs";
                return true;
            }
            if (ki.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
                hits = 0; misses = 0; quest = 0; spawnTarget(); status = "Hover the dot";
                return true;
            }
        }
        if (ki.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(ki);
    }

    @Override
    public void close() {
        NeuroDataset.forceSave();
        if (client != null) client.setScreen(new NeforTitleScreen());
    }
}
