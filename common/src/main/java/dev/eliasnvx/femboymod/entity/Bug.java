package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.config.CommonConfig;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bug: a small glitchy beetle that swarms at night (the "there's a bug in the code" meme). Weak alone,
 * annoying in a pack. Stylish players take less damage from it and programming socks hit it harder
 * ({@code data/femboymod/femboymod/drip_damage/bugs.json}, {@code cosmetic_stats/programming_socks.json}).
 */
public class Bug extends Monster {

    private static final float LEAP_HEIGHT = 0.3F;
    private static final double CHASE_SPEED = 1.2;
    private static final float LOOK_DISTANCE = 8.0F;

    public Bug(EntityType<? extends Bug> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(CommonConfig.Mob config) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, config.health())
                .add(Attributes.ATTACK_DAMAGE, config.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, config.speed());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, LEAP_HEIGHT));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, CHASE_SPEED, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.4F);
    }
}
