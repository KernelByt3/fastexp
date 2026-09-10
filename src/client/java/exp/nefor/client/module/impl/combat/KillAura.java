package exp.nefor.client.module.impl.combat;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.ChoiceSetting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.module.impl.movement.WindHop;
import exp.nefor.client.system.rotation.RotationEngine;
import exp.nefor.client.system.rotation.RotationProfile;
import exp.nefor.client.system.rotation.ServerType;
import exp.nefor.client.util.client.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {

    private final SliderSetting range = new SliderSetting("Радиус", 2.8, 3.1, 0.05, 3.0);
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты", true);
    private final BooleanSetting maceSpam = new BooleanSetting("Спам мейсом", true);
    private final ChoiceSetting rotationMode = new ChoiceSetting("Ротация", List.of("Auto", "Vanilla", "Hypixel", "ReallyWorld", "FunTime", "HolyWorld", "SpookyTime"), 0);
    private final ChoiceSetting aimPoint = new ChoiceSetting("Точка", List.of("Авто", "Голова", "Грудь", "Ноги", "Ближайшая"), 4);
    private final BooleanSetting neuroLearn = new BooleanSetting("Нейро (твои движения)", false);
    private final BooleanSetting keepSprint = new BooleanSetting("KeepSprint", false);
    private final BooleanSetting rotateCamera = new BooleanSetting("Камера", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", false);
    private final ChoiceSetting targetMode = new ChoiceSetting("Приоритет", List.of("Ближайший","Здоровье","Угол"), 0);
    private final BooleanSetting humanize = new BooleanSetting("Гуманность", true);

    private LivingEntity target;
    private boolean attackedThisJump;
    private long lastAttackTime;
    private long targetSince = 0;
    private long reactionMs = 120;
    private float overYaw = 0f;
    private float overPitch = 0f;

    public KillAura() {
        super("KillAura", "Убивает", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(range, onlyCrits, maceSpam, rotationMode, aimPoint, neuroLearn, keepSprint, throughWalls, targetMode, rotateCamera, humanize);
    }

    private RotationProfile currentProfile() {
        String sel = rotationMode.getValue();
        if ("Auto".equals(sel)) return RotationProfile.byServer(ServerType.detect());
        return RotationProfile.byName(sel);
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        var other = exp.nefor.client.module.ModuleManager.get(
                exp.nefor.client.module.impl.combat.NeuroAura.class);
        if (other != null && other.isEnabled()) other.setEnabled(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        attackedThisJump = false;
        lastAttackTime = 0;
        exp.nefor.client.system.rotation.SmoothRotationManager.hold(300);
    }

    @Override
    public void onTick() {
        
        
        
        float srvYaw = exp.nefor.client.system.rotation.SmoothRotationManager.getYaw();
        float srvPitch = exp.nefor.client.system.rotation.SmoothRotationManager.getPitch();
        RotationUtil.onClientTick();
        exp.nefor.client.system.rotation.SmoothRotationManager.tick();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null || client.interactionManager == null) return;

        ClientWorld world = client.world;
        ItemStack stack = player.getMainHandStack();
        boolean isMace = stack.isOf(Items.MACE);

        if (player.isOnGround()) {
            attackedThisJump = false;
        }

        
        var windHop = exp.nefor.client.module.ModuleManager.get(WindHop.class);
        if(windHop!=null && windHop.isActive()){
            return;
        }
        
        if(System.currentTimeMillis() - WindHop.lastWindMs < 900) return;
        boolean holdingWind = player.getMainHandStack().isOf(Items.WIND_CHARGE) || player.getOffHandStack().isOf(Items.WIND_CHARGE);
        if(holdingWind && (player.isUsingItem() || player.getPitch() > 70 || client.options.useKey.isPressed())){
            exp.nefor.client.system.rotation.SmoothRotationManager.hold(500);
            return;
        }

        LivingEntity prevTarget = target;
        if (target != null && target.isAlive() && !target.isRemoved() && player.distanceTo(target) <= range.getValue()+1.0 && exp.nefor.client.util.RaycastUtil.canHit(player, target, range.getValue()+0.5)) {
            
        } else {
            target = findTarget(player, world, range.getValue());
        }
        if (target == null) {
            exp.nefor.client.system.rotation.SmoothRotationManager.hold(800);
            return;
        }
        long nowMs = System.currentTimeMillis();
        exp.nefor.client.system.rotation.SmoothRotationManager.humanize = humanize.getValue();
        if (target != prevTarget) {
            targetSince = nowMs;
            var rnd = ThreadLocalRandom.current();
            reactionMs = 90 + rnd.nextInt(130);
            overYaw = (2f + rnd.nextFloat() * 3f) * (rnd.nextBoolean() ? 1f : -1f);
            overPitch = (1f + rnd.nextFloat() * 2f) * (rnd.nextBoolean() ? 1f : -1f);
        }
        boolean reacting = humanize.getValue() && nowMs - targetSince < reactionMs;
        overYaw *= 0.88f;
        overPitch *= 0.88f;

        RotationProfile profile = currentProfile();
        
        double leadSec = MathHelper.clamp(player.distanceTo(target) * 0.08, 0.05, 0.28);

        
        
        double maxReach = Math.min(range.getValue(), 3.0);
        if (!keepSprint.getValue() && player.distanceTo(target) <= maxReach + 0.5) {
            player.setSprinting(false);
            client.options.sprintKey.setPressed(false);
        }
        
        if (reacting) {
            
        } else if(neuroLearn.getValue() && exp.nefor.client.system.neural.NeuroDataset.size() >= 80 && exp.nefor.client.system.neural.NeuroModel.get().epochsTrained >= 15){            float[] ang = RotationUtil.getRotations(target, leadSec);
            float dYaw = net.minecraft.util.math.MathHelper.wrapDegrees(ang[0] - player.getYaw());
            float dPitch = ang[1] - player.getPitch();
            float dist = (float)player.distanceTo(target);
            float factor = exp.nefor.client.system.neural.NeuroModel.get().predict(dYaw, dPitch, dist);
            float nextYaw = player.getYaw() + net.minecraft.util.math.MathHelper.clamp(dYaw * factor, -18f, 18f);
            float nextPitch = net.minecraft.util.math.MathHelper.clamp(player.getPitch() + dPitch*factor, -90f, 90f);
            float gcd = exp.nefor.client.system.rotation.GcdUtil.getGcd();
            nextYaw = exp.nefor.client.system.rotation.GcdUtil.snapAngle(player.getYaw(), nextYaw, gcd);
            nextPitch = exp.nefor.client.system.rotation.GcdUtil.snapAngle(player.getPitch(), nextPitch, gcd);
            exp.nefor.client.system.rotation.SmoothRotationManager.setTargetWithFactor(nextYaw, nextPitch, factor);
        } else {
            RotationEngine.rotateTo(target, profile, leadSec, overYaw, overPitch);
            
            if(neuroLearn.getValue() && player.age % 80 == 0){
                net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(net.minecraft.text.Text.literal("§7[Neuro] нужно 5+ мин тренировки: "+exp.nefor.client.system.neural.NeuroDataset.size()+"/80, epochs "+exp.nefor.client.system.neural.NeuroModel.get().epochsTrained+"/15 — иди в Title → Нейро Тренировка"), true);
            }
        }

        // видимый доворот камеры за прицелом (пакеты те же, разница только в картинке)
        if (rotateCamera.getValue() && RotationUtil.isRotating) {
            player.setYaw(RotationUtil.targetYaw);
            player.setPitch(RotationUtil.targetPitch);
        }

        
        
        double eyeDist = player.getEyePos().distanceTo(exp.nefor.client.util.RaycastUtil.closestPoint(target.getBoundingBox(), player.getEyePos()));
        if (eyeDist > maxReach) return;
        if (player.distanceTo(target) > maxReach + 0.3) return;

        boolean moving = player.getVelocity().horizontalLength() > 0.08 || client.options.forwardKey.isPressed() || client.options.leftKey.isPressed() || client.options.rightKey.isPressed();
        float aimFov = moving ? 28f : currentProfile().fovCheck;
        if (!RotationUtil.isLookingAt(target, aimFov)) return;

        
        
        
        if (!exp.nefor.client.util.RaycastUtil.isAimingAt(player, srvYaw, srvPitch, target, maxReach)) return;

        if (player.isUsingItem() || player.isBlocking()) return;
        
        if (exp.nefor.client.util.client.MultiActionsBypass.isActive() && !moving) return;

        long now = System.currentTimeMillis();

        if (isMace && maceSpam.getValue()) {
            
            if (player.fallDistance > 1.5F) {
                doAttack(client, player, target, srvYaw, srvPitch);
                attackedThisJump = true;
                lastAttackTime = now;
            } else if (now - lastAttackTime >= 50 + ThreadLocalRandom.current().nextInt(30)) {
                doAttack(client, player, target, srvYaw, srvPitch);
                attackedThisJump = true;
                lastAttackTime = now;
            }
        } else {
            float cd = player.getAttackCooldownProgress(0.0f);
            
            if (onlyCrits.getValue()) {
                if (cd < 0.995f) return;
                if (attackedThisJump) return;
                if (player.isOnGround()) return;
                if (player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) return;
                if (player.fallDistance <= 0.0F) return;
                if (player.isSprinting()) {
                    
                    
                    player.setSprinting(false);
                    client.options.sprintKey.setPressed(false);
                }
            } else {
                if (cd < 0.90f) return;
            }
            doAttack(client, player, target, srvYaw, srvPitch);
            attackedThisJump = true;
        }
    }

    private void doAttack(MinecraftClient client, ClientPlayerEntity player, LivingEntity target, float srvYaw, float srvPitch) {
        float ry = player.getYaw();
        float rp = player.getPitch();
        
        
        player.setYaw(srvYaw);
        player.setPitch(srvPitch);
        if(!keepSprint.getValue()){
            player.setSprinting(false);
            client.options.sprintKey.setPressed(false);
        }
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        player.setYaw(ry);
        player.setPitch(rp);
    }

    private boolean canCrit(ClientPlayerEntity player) {
        if (attackedThisJump) return false;
        if (player.isOnGround()) return false;
        if (player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) return false;
        
        return player.fallDistance > 0.0F;
    }

    private LivingEntity findTarget(ClientPlayerEntity player, ClientWorld world, double maxRange) {
        maxRange = Math.min(maxRange, 3.0);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;        for (var entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == player || !living.isAlive() || living.isRemoved()) continue;
            if (entity instanceof net.minecraft.entity.player.PlayerEntity pe && exp.nefor.client.system.FriendManager.isFriend(pe.getName().getString())) continue;
            if (living instanceof net.minecraft.entity.player.PlayerEntity pe2 && pe2.isCreative()) continue;
            if (living.hurtTime>0) continue;
            if (!throughWalls.getValue() && !exp.nefor.client.util.RaycastUtil.canHit(player, living, maxRange+0.3)) continue;
            double eyeDist = player.getEyePos().distanceTo(exp.nefor.client.util.RaycastUtil.closestPoint(living.getBoundingBox(), player.getEyePos()));
            if (eyeDist > maxRange) continue;
            double score;
            String mode = targetMode.getValue();
            if ("Здоровье".equals(mode)) score = living.getHealth() * 10 + eyeDist;
            else if ("Угол".equals(mode)){
                float[] ang = exp.nefor.client.util.client.RotationUtil.getRotations(living);
                float yawDiff = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(ang[0]-player.getYaw()));
                score = yawDiff + eyeDist*2;
            } else score = eyeDist;
            if (score < bestScore) { best = living; bestScore = score; }
        }
        return best;
    }
}