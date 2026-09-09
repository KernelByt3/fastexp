package exp.nefor.client.module.impl.player;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/**
 * FakePlayer — NPC для теста KillAura: можно бить, бесконечные тотемы, киллаура его видит.
 */
public class FakePlayer extends Module {

    private OtherClientPlayerEntity fake;
    private int totemsPopped = 0;
    private Vec3d spawnPos;

    public FakePlayer(){
        super("FakePlayer", "NPC для теста ауры с тотемами", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable(){
        spawn();
    }

    @Override
    public void onDisable(){
        despawn();
    }

    private void spawn(){
        var mc = MinecraftClient.getInstance();
        if(mc.player==null || mc.world==null) return;
        despawn();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "FakePlayer");
        fake = new OtherClientPlayerEntity(mc.world, profile);
        // 2 блока перед игроком
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector();
        Vec3d pos = eye.add(look.x*2.2, 0, look.z*2.2);
        // на землю
        pos = new Vec3d(pos.x, mc.player.getY(), pos.z);
        spawnPos = pos;
        fake.setPosition(pos.x, pos.y, pos.z);
        fake.setYaw(mc.player.getYaw()+180);
        fake.setBodyYaw(mc.player.getYaw()+180);
        fake.setHealth(20f);
        fake.setAbsorptionAmount(0);
        fake.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, Items.NETHERITE_HELMET.getDefaultStack());
        fake.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE.getDefaultStack());
        fake.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, Items.NETHERITE_LEGGINGS.getDefaultStack());
        fake.equipStack(net.minecraft.entity.EquipmentSlot.FEET, Items.NETHERITE_BOOTS.getDefaultStack());
        fake.setStackInHand(Hand.MAIN_HAND, Items.NETHERITE_SWORD.getDefaultStack());
        fake.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
        fake.setCustomName(Text.literal("FakePlayer §7[ §a∞ totems §7]"));
        fake.setCustomNameVisible(true);
        mc.world.addEntity(fake);
        totemsPopped = 0;
        RenderSystem.notification("FakePlayer заспавнен — киллаура будет бить", Color.GREEN);
    }

    private void despawn(){
        var mc = MinecraftClient.getInstance();
        if(fake!=null && mc.world!=null){
            mc.world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        }
        fake = null;
        spawnPos = null;
    }

    @Override
    public void onTick(){
        var mc = MinecraftClient.getInstance();
        if(!isEnabled() || fake==null || mc.world==null || mc.player==null) return;

        // держим на месте, не падает
        fake.setVelocity(0,0,0);
        fake.setOnGround(true);
        // бесконечные тотемы — если хп <=2, попустить тотем
        if(fake.getHealth() <= 2f){
            fake.setHealth(20f);
            fake.clearStatusEffects();
            fake.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.REGENERATION, 80, 1));
            fake.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.ABSORPTION, 100, 0));
            fake.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
            // партикли тотема — убрано для совместимости
            totemsPopped++;
            fake.setCustomName(Text.literal("FakePlayer §7[ §a"+totemsPopped+" §7 totems ]"));
            fake.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
        }

        // если отошёл далеко — тп к игроку
        if(mc.player.distanceTo(fake) > 12){
            Vec3d eye = mc.player.getEyePos();
            Vec3d look = mc.player.getRotationVector();
            Vec3d pos = eye.add(look.x*2.2, 0, look.z*2.2);
            pos = new Vec3d(pos.x, mc.player.getY(), pos.z);
            fake.setPosition(pos.x, pos.y, pos.z);
        }

        // чтобы киллаура точно била — делаем хитбокс видимым и не в креативе
        fake.setInvisible(false);
        fake.setInvulnerable(false);
    }

    public LivingEntity getFake(){ return fake; }
}
