package exp.nefor.client.module.impl.combat;

import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.system.neural.NeuroDataset;
import exp.nefor.client.system.neural.NeuroModel;
import exp.nefor.client.system.rotation.SmoothRotationManager;
import exp.nefor.client.util.RaycastUtil;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;





public class NeuroAura extends Module {
    private final BooleanSetting neuro = new BooleanSetting("Нейро", true);
    private final SliderSetting range = new SliderSetting("Радиус", 2.8, 4.5, 0.1, 3.2);
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты", true);
    private final BooleanSetting rotateCamera = new BooleanSetting("Камера", false);
    private final BooleanSetting humanize = new BooleanSetting("Гуманность", true);
    private final BooleanSetting autoTrain = new BooleanSetting("Авто-дообучение", false);

    private LivingEntity target;
    private long aimLockMs = 0;
    private long reactionMs = 120;
    private float overYaw = 0f;
    private float overPitch = 0f;
    private float lastDeltaYaw, lastDeltaPitch, lastDist;

    public NeuroAura() {
        super("NeuroAura", "Нейро-аура обучаемая", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(neuro, range, onlyCrits, rotateCamera, humanize, autoTrain);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        var other = exp.nefor.client.module.ModuleManager.get(
                exp.nefor.client.module.impl.combat.KillAura.class);
        if (other != null && other.isEnabled()) other.setEnabled(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        SmoothRotationManager.hold(300);
    }

    @Override
    public void onTick() {
        
        
        float srvYaw = SmoothRotationManager.getYaw();
        float srvPitch = SmoothRotationManager.getPitch();
        RotationUtil.onClientTick();
        SmoothRotationManager.tick();
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        ClientPlayerEntity player = mc.player;

        
        var windHop = exp.nefor.client.module.ModuleManager.get(
                exp.nefor.client.module.impl.movement.WindHop.class);
        if (windHop != null && windHop.isActive()) return;
        if (System.currentTimeMillis() - exp.nefor.client.module.impl.movement.WindHop.lastWindMs < 900) return;

        double maxReach = Math.min(range.getValue(), 3.0);
        LivingEntity prev = target;
        if (target != null && target.isAlive() && !target.isRemoved()
                && player.distanceTo(target) <= range.getValue() + 1.0
                && RaycastUtil.canHit(player, target, range.getValue() + 0.5)) {
            
        } else {
            target = findTarget(player, range.getValue());
        }
        if (target == null) {
            SmoothRotationManager.hold(800);
            return;
        }
        long nowMs = System.currentTimeMillis();
        SmoothRotationManager.humanize = humanize.getValue();
        if (target != prev) {
            aimLockMs = nowMs;
            var rnd = java.util.concurrent.ThreadLocalRandom.current();
            reactionMs = 90 + rnd.nextInt(130);
            overYaw = (2f + rnd.nextFloat() * 3f) * (rnd.nextBoolean() ? 1f : -1f);
            overPitch = (1f + rnd.nextFloat() * 2f) * (rnd.nextBoolean() ? 1f : -1f);
        }
        boolean reacting = humanize.getValue() && nowMs - aimLockMs < reactionMs;
        overYaw *= 0.88f;
        overPitch *= 0.88f;

        
        
        if (player.distanceTo(target) <= maxReach + 0.5) {
            player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
        }

        float[] ang = RotationUtil.getRotations(target,
                MathHelper.clamp(player.distanceTo(target) * 0.08, 0.05, 0.28));
        float baseYaw = RotationUtil.isRotating ? RotationUtil.targetYaw : player.getYaw();
        float basePitch = RotationUtil.isRotating ? RotationUtil.targetPitch : player.getPitch();
        float deltaYaw = MathHelper.wrapDegrees(ang[0] - baseYaw);
        float deltaPitch = ang[1] - basePitch;
        float dist = (float) player.distanceTo(target);
        lastDeltaYaw = deltaYaw;
        lastDeltaPitch = deltaPitch;
        lastDist = dist;

        float factor;
        if (neuro.getValue() && NeuroDataset.size() >= 1) {
            factor = MathHelper.clamp(NeuroModel.get().predict(deltaYaw, deltaPitch, dist), 0.16f, 0.38f);
        } else {
            factor = 0.26f;
        }

        if (!reacting) {
            SmoothRotationManager.setTargetWithFactor(
                    baseYaw + deltaYaw + overYaw, basePitch + deltaPitch + overPitch, factor);
        }

        // видимый доворот камеры за прицелом (пакеты те же, разница только в картинке)
        if (rotateCamera.getValue() && RotationUtil.isRotating) {
            player.setYaw(RotationUtil.targetYaw);
            player.setPitch(RotationUtil.targetPitch);
        }

        
        if (player.distanceTo(target) > maxReach + 0.3) return;
        double eyeDist = player.getEyePos().distanceTo(
                RaycastUtil.closestPoint(target.getBoundingBox(), player.getEyePos()));
        if (eyeDist > maxReach) return;

        boolean moving = player.getVelocity().horizontalLength() > 0.08
                || mc.options.forwardKey.isPressed() || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
        if (!RotationUtil.isLookingAt(target, moving ? 10f : 6f)) return;

        
        
        if (!RaycastUtil.isAimingAt(player, srvYaw, srvPitch, target, maxReach)) return;

        if (player.getAttackCooldownProgress(0) < 0.995f) return;
        
        
        if (onlyCrits.getValue()) {
            if (player.isOnGround()) return;
            if (player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) return;
            if (player.fallDistance <= 0.0F) return;
            if (player.isSprinting()) {
                player.setSprinting(false);
                mc.options.sprintKey.setPressed(false);
            }
        }

        float ry = player.getYaw(), rp = player.getPitch();
        player.setYaw(srvYaw);
        player.setPitch(srvPitch);
        player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
        mc.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        player.setYaw(ry);
        player.setPitch(rp);

        
        if (autoTrain.getValue()) {
            long reaction = MathHelper.clamp(System.currentTimeMillis() - aimLockMs, 30, 1500);
            NeuroDataset.add(new NeuroDataset.Sample(lastDeltaYaw, lastDeltaPitch, lastDist, lastDist, reaction, true));
            if (player.age % 60 == 0) NeuroModel.get().train(2);
        }
    }

    private LivingEntity findTarget(ClientPlayerEntity p, double r) {
        double maxRange = Math.min(r, 3.0);
        LivingEntity best = null;
        double bd = maxRange;
        var world = MinecraftClient.getInstance().world;
        if (world == null) return null;
        for (var e : world.getEntities()) {
            if (!(e instanceof LivingEntity l)) continue;
            if (e == p || !l.isAlive() || l.isRemoved()) continue;
            if (e instanceof net.minecraft.entity.player.PlayerEntity pe
                    && exp.nefor.client.system.FriendManager.isFriend(pe.getName().getString())) continue;
            if (l instanceof net.minecraft.entity.player.PlayerEntity pe2 && pe2.isCreative()) continue;
            if (l.hurtTime > 0) continue;
            if (l.getType().toString().contains("ArmorStand")) continue;
            if (!RaycastUtil.canHit(p, l, maxRange + 0.3)) continue;
            double d = p.getEyePos().distanceTo(RaycastUtil.closestPoint(l.getBoundingBox(), p.getEyePos()));
            if (d > maxRange) continue;
            if (d < bd) { best = l; bd = d; }
        }
        return best;
    }

    public LivingEntity getTarget() { return target; }
}
