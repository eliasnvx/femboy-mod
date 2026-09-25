package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.config.CommonConfig;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Hissy Cat: a black cat with glowing eyes that stalks forests at night, pounces and scratches.
 * Cats are cute; this one simply is not in the mood. Not tameable (it is a monster, not a vanilla cat).
 */
public class HissyCat extends Monster {

    private static final float POUNCE_HEIGHT = 0.4F;
    private static final double CHASE_SPEED = 1.25;
    private static final float LOOK_DISTANCE = 10.0F;
    private static final float VOICE_PITCH = 0.8F;
    /** Cats land on their feet. */
    private static final double CAT_SAFE_FALL = 6.0;

    public HissyCat(EntityType<? extends HissyCat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(CommonConfig.Mob config) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, config.health())
                .add(Attributes.ATTACK_DAMAGE, config.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, config.speed())
                .add(Attributes.SAFE_FALL_DISTANCE, CAT_SAFE_FALL);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, POUNCE_HEIGHT));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, CHASE_SPEED, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_HISS_BABY.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH + (random.nextFloat() - random.nextFloat()) * 0.1F;
    }
}
