package exp.nefor.client.module.impl.combat;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.system.neural.NeuroDataset;
import exp.nefor.client.system.neural.NeuroModel;
import exp.nefor.client.system.rotation.GcdUtil;
import exp.nefor.client.system.rotation.RotationProfile;
import exp.nefor.client.system.rotation.ServerType;
import exp.nefor.client.system.rotation.SmoothRotationManager;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Нейро KillAura — использует NeuroModel обученный на твоих квестах.
 * Датасет собирается в NeuralTrainingScreen, epochs обучают веса.
 */
public class NeuroAura extends Module {
    private final BooleanSetting neuro = new BooleanSetting("Нейро", true);
    private final SliderSetting range = new SliderSetting("Радиус", 2.8, 4.5, 0.1, 3.2);
    private final BooleanSetting autoTrain = new BooleanSetting("Авто-дообучение", false);

    private LivingEntity target;

    public NeuroAura(){
        super("NeuroAura", "Нейро-аура обучаемая", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(neuro, range, autoTrain);
    }

    @Override public void onTick(){
        RotationUtil.onClientTick();
        SmoothRotationManager.tick();
        var mc = MinecraftClient.getInstance();
        if(mc.player==null || mc.world==null) return;
        if(target!=null && target.isAlive() && !target.isRemoved() && mc.player.distanceTo(target) <= range.getValue()+1.0 && exp.nefor.client.util.RaycastUtil.canHit(mc.player, target, range.getValue()+0.5)){
        } else target = findTarget(mc.player, range.getValue());
        if(target==null){ SmoothRotationManager.reset(); RotationUtil.reset(); return; }

        float[] ang = RotationUtil.getRotations(target);
        float deltaYaw = MathHelper.wrapDegrees(ang[0] - mc.player.getYaw());
        float deltaPitch = ang[1] - mc.player.getPitch();
        float dist = (float)mc.player.distanceTo(target);

        float factor;
        if(neuro.getValue() && NeuroDataset.size() >= 1){
            factor = NeuroModel.get().predict(deltaYaw, deltaPitch, dist);
            var samples = NeuroDataset.all();
            if(!samples.isEmpty()){
                var s = samples.get((int)(Math.random()*samples.size()));
                deltaYaw += s.deltaYaw()*0.35f;
                deltaPitch += s.deltaPitch()*0.35f;
            }
            if(autoTrain.getValue() && mc.player.age % 60 == 0) NeuroModel.get().train(1);
        } else {
            factor = 0.24f; // дефолт без датасета — быстрее чтобы наводилось
        }

        // напрямую к цели с нейро-фактором — без двойного сглаживания
        float targetYaw = mc.player.getYaw() + deltaYaw;
        float targetPitch = mc.player.getPitch() + deltaPitch;
        SmoothRotationManager.setTargetWithFactor(targetYaw, targetPitch, factor);

        if(mc.player.distanceTo(target) > range.getValue()+0.3) return;
        boolean movingN = mc.player.getVelocity().horizontalLength() > 0.08 || mc.options.forwardKey.isPressed() || mc.options.leftKey.isPressed();
        float fovN = movingN ? 24f : 14f;
        if(!RotationUtil.isLookingAt(target, fovN)) return;
        if(mc.player.getAttackCooldownProgress(0) < 0.995f) return;
        // атака
        float ry = mc.player.getYaw(), rp = mc.player.getPitch();
        mc.player.setYaw(SmoothRotationManager.getYaw());
        mc.player.setPitch(SmoothRotationManager.getPitch());
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        mc.player.setYaw(ry); mc.player.setPitch(rp);
    }

    private LivingEntity findTarget(net.minecraft.client.network.ClientPlayerEntity p, double r){
        LivingEntity best=null; double bd=r+0.5;
        var world = net.minecraft.client.MinecraftClient.getInstance().world;
        if(world==null) return null;
        for(var e: world.getEntities()){
            if(!(e instanceof LivingEntity l)) continue;
            if(e==p || !l.isAlive() || l.isRemoved()) continue;
            if(l.getType().toString().contains("ArmorStand")) continue;
            if(l.hurtTime>0) continue;
            if (!exp.nefor.client.util.RaycastUtil.canHit(p, l, r+0.3)) continue;
            double d=p.getEyePos().distanceTo(exp.nefor.client.util.RaycastUtil.closestPoint(l.getBoundingBox(), p.getEyePos()));
            if(d > r+0.05) continue;
            if(d<=bd){ best=l; bd=d; }
        }
        return best;
    }
    public LivingEntity getTarget(){ return target; }
}
