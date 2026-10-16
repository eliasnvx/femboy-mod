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
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

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
    private static final int BOSS_BAR_UPDATE_TICKS = 20;
    private static final int XP_REWARD = 30;
    private static final float VOICE_PITCH = 0.85F;
    private static final float REVIEW_PITCH = 0.8F;
    /** Player-like eyes (1.21 used {@code EntityType.Builder#eyeHeight}). */
    private static final float EYE_HEIGHT = 1.62F;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_6);
    private int reviewCooldown;
    private int idleTicks;

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
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return EYE_HEIGHT;
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
        if (tickCount % BOSS_BAR_UPDATE_TICKS == 0) {
            refreshBossBar();
        }
        if (tickIdle(FemboyConfig.common().mobs().criticReview().idleDespawnTicks())) {
            discard();
            return;
        }
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
        var worn = CosmeticsManager.worn(player);
        for (var entry : worn.all().entrySet()) {
            if (!worn.isHidden(entry.getKey()) && entry.getValue().is(FemboyTags.CRITIC_APPROVED)) {
                return true;
            }
        }
        return false;
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

    /**
     * Counts the ticks without a target; true once {@code limit} of them are over (0 = never), so critics that
     * spawned somewhere nobody fights them (under a base) do not stay around forever.
     */
    public boolean tickIdle(int limit) {
        if (getTarget() != null) {
            idleTicks = 0;
            return false;
        }
        return limit > 0 && ++idleTicks >= limit;
    }

    /**
     * Natural spawning: only on the surface (not in caves under a base) in the dark, and not near another critic, so
     * a long-lived world does not collect them.
     */
    public static boolean checkSpawnRules(EntityType<FashionCritic> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random) || !level.canSeeSky(pos)) {
            return false;
        }
        return noCriticWithin(level, pos, FemboyConfig.common().mobs().criticReview().spawnSpacing());
    }

    /** @return true if no critic is within {@code spacing} blocks of {@code pos} (always true for 0) */
    public static boolean noCriticWithin(LevelAccessor level, BlockPos pos, int spacing) {
        return spacing <= 0 || level.getEntitiesOfClass(FashionCritic.class, new AABB(pos).inflate(spacing)).isEmpty();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        // Stays while it fights; otherwise despawns like any monster instead of piling up around bases
        return getTarget() == null;
    }

    /** The boss bar belongs to the fight: only the player this critic is after sees it. */
    public void refreshBossBar() {
        showBossBarTo(getTarget() instanceof ServerPlayer player && isAlive() ? player : null);
    }

    /** Shows the boss bar to {@code viewer} only ({@code null}: to nobody). */
    public void showBossBarTo(ServerPlayer viewer) {
        for (ServerPlayer shown : List.copyOf(bossBar.getPlayers())) {
            if (shown != viewer) {
                bossBar.removePlayer(shown);
            }
        }
        if (viewer != null) {
            bossBar.addPlayer(viewer);
        }
    }

    /** @return the players currently shown the boss bar (for GameTests) */
    public Collection<ServerPlayer> bossBarPlayers() {
        return bossBar.getPlayers();
    }
}
