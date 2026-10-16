package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.block.RubberDuckBlock;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Rubber Duck (rubber duck debugging): waddles on river banks and beaches, floats high on water like the toy, squeaks.
 * Tamed with seeds ({@code #femboymod:rubber_duck_food}), it follows its owner; sneak-right-click makes it sit.
 * Explaining your code to your tamed duck (right-click with an empty hand) is a debugging session like with the Rubber
 * Duck block
 * ({@link RubberDuckBlock#debug}: Insight, experience, the shared cooldown), and the live duck also fixes one bug: a
 * harmful effect goes away.
 */
public class RubberDuck extends TamableAnimal {

    private static final double MAX_HEALTH = 6.0;
    private static final double SPEED = 0.25;
    private static final double PANIC_SPEED = 1.4;
    private static final double TEMPT_SPEED = 1.1;
    private static final double FOLLOW_SPEED = 1.1;
    private static final float FOLLOW_START = 6.0F;
    private static final float FOLLOW_STOP = 2.0F;
    private static final float LOOK_DISTANCE = 6.0F;
    private static final byte TAME_HEARTS = 7;
    private static final byte TAME_SMOKE = 6;
    private static final int PARTICLES = 5;
    /**
     * Floating like the toy: a spring pulls the duck to {@link #DRAFT} blocks under the surface (of its 0.5), damped so
     * it settles without bobbing. Vanilla floating (FloatGoal jumps) keeps a mob this short 0.4 deep and bobbing.
     */
    private static final double DRAFT = 0.15;
    private static final double BUOYANCY = 0.15;
    private static final double FLOAT_DAMPING = 0.4;
    /**
     * Eyes at 0.42 of its 0.5 height (1.21.1 sets 0.42 with {@code EntityType.Builder#eyeHeight}, which 1.20.1 doesn't
     * have); a fraction so ducklings, whose dimensions are halved, keep their eyes on their smaller head too.
     */
    private static final float EYE_HEIGHT_FRACTION = 0.42F / 0.5F;

    public RubberDuck(EntityType<? extends RubberDuck> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 1.20.1 TemptGoal has a fixed range (26.3 reads the animal attribute tempt_range)
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED);
    }

    /** On the ground (#femboymod:rubber_duck_spawnable_on), in daylight, like other animals. */
    public static boolean checkDuckSpawnRules(EntityType<RubberDuck> type, LevelAccessor level, MobSpawnType reason,
                                              BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(FemboyTags.RUBBER_DUCK_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * EYE_HEIGHT_FRACTION;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, PANIC_SPEED));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        // 1.20.1 TemptGoal takes an Ingredient instead of a predicate: the same food tag
        goalSelector.addGoal(4, new TemptGoal(this, TEMPT_SPEED, Ingredient.of(FemboyTags.RUBBER_DUCK_FOOD), false));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, FOLLOW_SPEED, FOLLOW_START, FOLLOW_STOP, false));
        goalSelector.addGoal(6, new FollowParentGoal(this, FOLLOW_SPEED));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    /**
     * 26.3 overrides {@code travelInFluid}; 1.20.1 (like 1.21.1) moves through water inside {@link #travel} (same
     * branch condition, same water physics), so the spring is applied after it, under the condition that branch ran with.
     */
    @Override
    public void travel(Vec3 input) {
        boolean inWater = isControlledByLocalInstance() && isInWater() && isAffectedByFluids()
                && !canStandOnFluid(level().getFluidState(blockPosition()));
        super.travel(input);
        // horizontalCollision: leave vanilla's hop out of the water onto a bank alone
        if (inWater && !horizontalCollision) {
            Vec3 motion = getDeltaMovement();
            double lift = (getFluidHeight(FluidTags.WATER) - DRAFT) * BUOYANCY;
            setDeltaMovement(motion.x, motion.y * FLOAT_DAMPING + lift, motion.z);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FemboyTags.RUBBER_DUCK_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isTame() && isFood(held)) {
            usePlayerItem(player, hand, held);
            if (level() instanceof ServerLevel) {
                if (random.nextFloat() < FemboyConfig.common().rubberDucks().tameChance()) {
                    tame(player);
                    navigation.stop();
                    setOrderedToSit(false);
                    level().broadcastEntityEvent(this, TAME_HEARTS);
                } else {
                    level().broadcastEntityEvent(this, TAME_SMOKE);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (player.isShiftKeyDown()) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                navigation.stop();
            } else if (level() instanceof ServerLevel level && player instanceof ServerPlayer owner) {
                debug(level, owner);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * A debugging session (server): a duck line, then, unless the shared cooldown runs, Insight and one harmful effect
     * removed.
     *
     * @return the effect it fixed, or null
     */
    public @Nullable MobEffectInstance debug(ServerLevel level, ServerPlayer owner) {
        playSound(FemboySounds.DUCK_SQUEAK.get(), 1.0F, 1.0F);
        owner.displayClientMessage(Component.translatable("message.femboymod.duck." + random.nextInt(RubberDuckBlock.LINES)), true);
        if (!RubberDuckBlock.debug(owner)) {
            return null;
        }
        MobEffectInstance bug = null;
        for (MobEffectInstance effect : owner.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                bug = effect;
                break;
            }
        }
        if (bug != null) {
            owner.removeEffect(bug.getEffect());
            owner.displayClientMessage(Component.translatable("message.femboymod.rubber_duck.fixed", getDisplayName(),
                    bug.getEffect().getDisplayName()), true);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, owner.getX(), owner.getEyeY(), owner.getZ(), PARTICLES, 0.3, 0.3, 0.3, 0.0);
        return bug;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        RubberDuck duckling = FemboyEntities.RUBBER_DUCK.get().create(level);
        if (duckling != null && isTame() && getOwner() != null) {
            duckling.setOwnerUUID(getOwnerUUID());
            duckling.setTame(true);
        }
        return duckling;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return FemboySounds.DUCK_QUACK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FemboySounds.DUCK_SQUEAK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FemboySounds.DUCK_DEATH.get();
    }

    /** Ducks don't take fall damage (they flap). */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
