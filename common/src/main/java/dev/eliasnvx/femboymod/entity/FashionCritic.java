package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.registry.FemboyTags;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.profile.StylePoints;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.event.profile.CriticReviewEvent;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Fashion Critic: a rare mini-boss in a beret and sunglasses. Hits unstylish players hard
 * ({@code drip_damage/fashion_critic.json}) and hands out "reviews": low Drip gets slowness and weakness,
 * high Drip impresses it (it gets weakness instead). Drops the exclusive Wolf Ears.
 */
public class FashionCritic extends Monster {

    private static final int REVIEW_LINES = 4;
    private static final double CHASE_SPEED = 1.0;
    private static final float LOOK_DISTANCE = 12.0F;
    private static final double KNOCKBACK_RESISTANCE = 0.6;
    private static final double FOLLOW_RANGE = 32.0;
    private static final int XP_REWARD = 30;
    private static final float VOICE_PITCH = 0.85F;
    private static final float REVIEW_PITCH = 0.8F;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_6);
    private int reviewCooldown;

    public FashionCritic(EntityType<? extends FashionCritic> type, Level level) {
        super(type, level);
        xpReward = XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes(CommonConfig.Mob config) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, config.health())
                .add(Attributes.ATTACK_DAMAGE, config.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, config.speed())
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, CHASE_SPEED, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK)); // the review notebook
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (reviewCooldown > 0) {
            reviewCooldown--;
            return;
        }
        CommonConfig.CriticReview config = FemboyConfig.common().mobs().criticReview();
        LivingEntity target = getTarget();
        if (target instanceof ServerPlayer player && distanceTo(player) <= config.range() && hasLineOfSight(player)) {
            review(player);
            reviewCooldown = config.cooldownTicks();
        }
    }

    /** Judges the player's outfit now (also used by GameTests). */
    public void review(ServerPlayer player) {
        CommonConfig.CriticReview config = FemboyConfig.common().mobs().criticReview();
        int tier = FemboyMod.api().getDripLevel(player).tier();
        boolean impressed = tier >= config.impressedTier() || wearsApproved(player);
        String verdict = impressed ? "impressed" : "unimpressed";
        int line = random.nextInt(REVIEW_LINES);
        player.displayClientMessage(Component.translatable("entity.femboymod.fashion_critic.review." + verdict + "." + line), true);
        if (impressed) {
            addEffect(new MobEffectInstance(MobEffects.WEAKNESS, config.effectTicks()));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, config.effectTicks(), config.slownessLevel()));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, config.effectTicks()));
        }
        playSound(impressed ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO, 1.0F, REVIEW_PITCH);

        PlayerProfile profile = FemboyMod.api().getProfile(player);
        profile.update(FemboyProfileFields.CRITIC_REVIEWS, count -> count + 1);
        if (impressed) {
            profile.update(FemboyProfileFields.CRITIC_PASSED, count -> count + 1);
            StylePoints.earn(player, FemboyConfig.common().stylePoints().criticPassed(), StylePoints.CRITIC_PASSED);
        }
        FemboyMod.api().events().post(new CriticReviewEvent(player, this, tier, impressed));
    }

    private static boolean wearsApproved(ServerPlayer player) {
        var worn = CosmeticsManager.get(player);
        for (var entry : worn.all().entrySet()) {
            if (!worn.isHidden(entry.getKey()) && entry.getValue().is(FemboyTags.CRITIC_APPROVED)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // a boss does not despawn mid-fight
    }
}
