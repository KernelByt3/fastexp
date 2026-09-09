package exp.nefor.client.gui;

import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.system.neural.NeuroDataset;
import exp.nefor.client.system.neural.NeuroModel;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

import java.util.Random;

/**
 * Меню обучения нейро-ауры: квесты наведения мышью.
 * Точка появляется, нужно навестись и кликнуть. Собирает датасет -> train epochs.
 */
public class NeuralTrainingScreen extends NeforScreen {
    private final Random rnd = new Random();
    private float targetX, targetY;
    private float targetR = 14f;
    private long spawnTime;
    private int hits = 0;
    private int misses = 0;
    private int quest = 0;
    private final int totalQuests = 60; // 5+ минут
    private String status = "Наведись на точку и кликни";
    private long lastHitMs = 0;

    public NeuralTrainingScreen(){ super(Text.literal("Neuro Training")); }

    @Override
    public void init(){
        super.init();
        spawnTarget();
    }

    private void spawnTarget(){
        targetX = 50 + rnd.nextFloat() * (width - 100);
        targetY = 50 + rnd.nextFloat() * (height - 100);
        targetR = 22 + rnd.nextFloat()*8; // заметно больше
        spawnTime = System.currentTimeMillis();
        quest++;
    }

    @Override
    public void renderNefor(){
        SakuraBackground.draw(System.currentTimeMillis());
        UiRender.dimScreen(width, height, 0.52f);

        // header
        UiRender.roundRect(width/2f - 180, 10, 360, 36, 10, new float[]{0.06f,0.06f,0.11f,0.95f}, new float[]{1,1,1,0.08f},1, new float[]{0.54f,0.17f,0.89f,0.18f},8);
        RenderSystem.drawText("NEURO TRAINING", width/2f - RenderSystem.textWidth("NEURO TRAINING",14f)/2, 18, 14f, 0xFF7D9BFF);
        RenderSystem.drawText("квест "+Math.min(quest,totalQuests)+"/"+totalQuests, width/2f - RenderSystem.textWidth("квест",8f)/2, 32, 8f, 0xFF9A9AA6);

        // progress bar
        float prog = (float)hits / totalQuests;
        UiRender.roundRect(width/2f - 100, 50, 200, 6, 3, new float[]{1,1,1,0.08f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        UiRender.roundRect(width/2f - 100, 50, 200*prog, 6, 3, new float[]{0.36f,0.49f,1f,1f}, new float[]{0,0,0,0},0, new float[]{0.36f,0.49f,1f,0.35f},6);

        if(quest > totalQuests){
            // финиш — авто-обучение уже шло
            UiRender.roundRect(width/2f - 160, height/2f - 55, 320, 110, 12, new float[]{0.06f,0.06f,0.11f,0.97f}, new float[]{1,1,1,0.09f},1, new float[]{0.54f,0.17f,0.89f,0.22f},10);
            RenderSystem.drawText("Тренировка завершена!", width/2f - RenderSystem.textWidth("Тренировка завершена!",13f)/2, height/2f - 35, 13f, 0xFFFFFFFF);
            String stat = "Попаданий: "+hits+" / "+totalQuests+"  промахов: "+misses;
            RenderSystem.drawText(stat, width/2f - RenderSystem.textWidth(stat,11f)/2, height/2f - 12, 11f, 0xFFEAEAF2);
            var model = NeuroModel.get();
            String loss = String.format("dataset: %d  epochs: %d  loss: %.5f", NeuroDataset.size(), model.epochsTrained, model.lastLoss);
            RenderSystem.drawText(loss, width/2f - RenderSystem.textWidth(loss,8f)/2, height/2f + 8, 8f, 0xFF9A9AA6);
            RenderSystem.drawText("ESC — выход  |  R — заново", width/2f - RenderSystem.textWidth("ESC — выход  |  R — заново",9f)/2, height/2f + 28, 9f, 0xFF7D9BFF);
            RenderSystem.drawText(status, width/2f - RenderSystem.textWidth(status,9f)/2, height - 22, 9f, 0xFF9A9AA6);
            return;
        }

        // авто-засчитывание при наведении — без клика
        double dxH = mouseX - targetX, dyH = mouseY - targetY;
        boolean hovering = Math.hypot(dxH, dyH) <= targetR;
        if(hovering && System.currentTimeMillis() - spawnTime > 80){
            long react = System.currentTimeMillis() - spawnTime;
            float dist = (float)Math.hypot(dxH, dyH);
            NeuroDataset.add(new NeuroDataset.Sample((float)dxH*0.08f,(float)dyH*0.08f, dist*0.04f, dist/(react+1f), react, true));
            hits++; status = "Попал! "+react+"ms — dataset "+NeuroDataset.size(); lastHitMs = System.currentTimeMillis();
            // авто-epochs каждые 3 попадания
            if(NeuroDataset.size() % 3 == 0) { NeuroModel.get().train(5); status += " | +5 epochs loss "+String.format("%.4f", NeuroModel.get().lastLoss); }
            if(hits >= totalQuests) quest = totalQuests+1;
            else spawnTarget();
        }

        // target — крупнее и заметнее
        long age = System.currentTimeMillis() - spawnTime;
        float pulse = 0.80f + 0.20f*(float)Math.sin(age*0.007);
        float outer = targetR + 10;
        UiRender.roundRect(targetX - outer, targetY - outer, outer*2, outer*2, 999, new float[]{0.36f,0.49f,1f,0.12f*pulse}, new float[]{0,0,0,0},0, new float[]{0.36f,0.49f,1f,0.30f},16);
        UiRender.roundRect(targetX - targetR, targetY - targetR, targetR*2, targetR*2, 999, hovering? new float[]{0.36f,0.49f,1f,1f}: new float[]{1,1,1,0.96f}, new float[]{0.36f,0.49f,1f,0.75f},3, new float[]{0.36f,0.49f,1f,0.30f},10);
        // центр
        UiRender.roundRect(targetX - 3, targetY - 3, 6,6, 3, hovering? new float[]{0.36f,0.49f,1f,1f}: new float[]{1,0.3f,0.3f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        // crosshair
        UiRender.roundRect((float)mouseX - 0.5f, (float)mouseY - 12, 1, 24, 0, hovering? new float[]{0.36f,0.49f,1f,1f}: new float[]{1,1,1,0.9f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        UiRender.roundRect((float)mouseX - 12, (float)mouseY - 0.5f, 24, 1, 0, hovering? new float[]{0.36f,0.49f,1f,1f}: new float[]{1,1,1,0.9f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        RenderSystem.drawText(status, width/2f - RenderSystem.textWidth(status,10f)/2, height - 22, 10f, 0xFF7D9BFF);
        if(lastHitMs>0 && System.currentTimeMillis()-lastHitMs<500){
            RenderSystem.drawText("HIT!", targetX+targetR+10, targetY-7, 13f, 0xFF4BE37A);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled){
        // теперь засчитывается наведением — клик только для промаха (штраф)
        if(quest > totalQuests) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        double dx = mx - targetX, dy = my - targetY;
        if(Math.hypot(dx,dy) > targetR){
            misses++; status = "Мимо — наведись точнее!";
            NeuroDataset.add(new NeuroDataset.Sample((float)dx*0.08f,(float)dy*0.08f, (float)Math.hypot(dx,dy)*0.04f, 0.5f, System.currentTimeMillis()-spawnTime, false));
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput ki){
        if(quest > totalQuests){
            if(ki.key()== org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER){
                NeuroModel.get().train(20);
                status = "Обучено 20 epochs! loss "+String.format("%.5f", NeuroModel.get().lastLoss);
                return true;
            }
            if(ki.key()== org.lwjgl.glfw.GLFW.GLFW_KEY_R){
                hits=0; misses=0; quest=0; spawnTarget(); status="Заново — наведись на точку";
                return true;
            }
        }
        if(ki.key()== org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){ close(); return true; }
        return super.keyPressed(ki);
    }

    @Override public void close(){ if(client!=null) client.setScreen(null); }
}
