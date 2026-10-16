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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
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

    public RubberDuck(EntityType<? extends RubberDuck> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // animal attributes include tempt_range, which TemptGoal reads
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED);
    }

    /** On the ground (#femboymod:rubber_duck_spawnable_on), in daylight, like other animals. */
    public static boolean checkDuckSpawnRules(EntityType<RubberDuck> type, LevelAccessor level, EntitySpawnReason reason,
                                              BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(FemboyTags.RUBBER_DUCK_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, PANIC_SPEED));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, TEMPT_SPEED, this::isFood, false));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, FOLLOW_SPEED, FOLLOW_START, FOLLOW_STOP));
        goalSelector.addGoal(6, new FollowParentGoal(this, FOLLOW_SPEED));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, LOOK_DISTANCE));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void travelInFluid(Vec3 input) {
        super.travelInFluid(input);
        // horizontalCollision: leave vanilla's hop out of the water onto a bank alone
        if (isInWater() && !horizontalCollision) {
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
            held.consume(1, player);
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
        owner.sendOverlayMessage(Component.translatable("message.femboymod.duck." + random.nextInt(RubberDuckBlock.LINES)));
        if (!RubberDuckBlock.debug(owner)) {
            return null;
        }
        MobEffectInstance bug = null;
        for (MobEffectInstance effect : owner.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                bug = effect;
                break;
            }
        }
        if (bug != null) {
            owner.removeEffect(bug.getEffect());
            owner.sendOverlayMessage(Component.translatable("message.femboymod.rubber_duck.fixed", getDisplayName(),
                    bug.getEffect().value().getDisplayName()));
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, owner.getX(), owner.getEyeY(), owner.getZ(), PARTICLES, 0.3, 0.3, 0.3, 0.0);
        return bug;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        RubberDuck duckling = FemboyEntities.RUBBER_DUCK.get().create(level, EntitySpawnReason.BREEDING);
        if (duckling != null && isTame() && getOwner() != null) {
            duckling.setOwnerReference(getOwnerReference());
            duckling.setTame(true, true);
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
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }
}
