package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Debug Flytrap (0.3.0): a swamp plant that catches bugs. Every few ticks it looks for prey
 * ({@code #femboymod:flytrap_prey}: the Bugs, silverfish, endermites) within reach, snaps shut on the nearest one and
 * eats it, then digests with its jaws closed. Every meal grows it a stage: three stages, each reaching further. Bone
 * meal does nothing. Named prey is left alone (a named bug is a feature).
 *
 * <p>The checks run on scheduled ticks, not a block entity, so the potted one stays a plain pot that cats can push
 * ({@link PottedDebugFlytrapBlock}). Placing it starts the loop and a random tick restarts a loop that got lost. 1.20.1's
 * {@code simple_block} feature can't schedule a tick (26.3's {@code schedule_tick}), so wild flytraps start hunting
 * with their first random tick. Numbers in config {@code plants.flytrap_*}.
 */
public class DebugFlytrapBlock extends BushBlock {

    public static final int MAX_STAGE = 2;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, MAX_STAGE);
    public static final BooleanProperty DIGESTING = BooleanProperty.create("digesting");
    /** Players this close see the meal (the advancement). */
    private static final double WITNESS_RANGE = 16.0;
    private static final int CRUMBS = 6;
    private static final double CRUMB_SPREAD = 0.15;
    private static final float SNAP_VOLUME = 0.8F;
    private static final float GULP_VOLUME = 0.6F;
    private static final VoxelShape[] SHAPES = {
            Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0)};

    public DebugFlytrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(DIGESTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, DIGESTING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)];
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && level instanceof ServerLevel server) {
            DebugFlytrapBlock.keepTicking(server, pos, state, this); // placed digesting (commands, structures): digests in full
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        keepTicking(level, pos, state, this);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        CommonConfig.Plants config = FemboyConfig.common().plants();
        if (state.getValue(DIGESTING)) {
            int stage = Math.min(state.getValue(STAGE) + 1, MAX_STAGE);
            finishDigesting(level, pos, state.setValue(STAGE, stage));
        } else if (!snap(level, pos, state, reach(state.getValue(STAGE), config))) {
            level.scheduleTick(pos, this, config.flytrapCheckInterval());
        }
    }

    /** How far a flytrap at this growth stage reaches (blocks from its center). */
    public static double reach(int stage, CommonConfig.Plants config) {
        return config.flytrapReach() + stage * config.flytrapReachPerStage();
    }

    /** Restarts the check loop if it got lost (random ticks; also wild flytraps and chunk upgrades). */
    static void keepTicking(ServerLevel level, BlockPos pos, BlockState state, Block block) {
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) {
            CommonConfig.Plants config = FemboyConfig.common().plants();
            level.scheduleTick(pos, block, state.getValue(DIGESTING) ? config.flytrapDigestTicks() : config.flytrapCheckInterval());
        }
    }

    /** The nearest prey within reach of the plant's center, or null. */
    public static @Nullable LivingEntity findPrey(Level level, BlockPos pos, double reach) {
        Vec3 center = Vec3.atCenterOf(pos);
        List<LivingEntity> prey = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(reach),
                entity -> entity.isAlive() && entity.getType().is(FemboyTags.FLYTRAP_PREY) && !entity.hasCustomName()
                        && !(entity instanceof Player) // even if a pack tags players: never remove one
                        && entity.distanceToSqr(center) <= reach * reach);
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : prey) {
            double distance = entity.distanceToSqr(center);
            if (distance < best) {
                nearest = entity;
                best = distance;
            }
        }
        return nearest;
    }

    /**
     * Eats the nearest prey within reach, if any: the jaws close (the state gets {@link #DIGESTING}) and the digest
     * tick is scheduled. Returns whether it ate. The state's block decides what happens when digestion ends.
     */
    public static boolean snap(ServerLevel level, BlockPos pos, BlockState state, double reach) {
        LivingEntity prey = findPrey(level, pos, reach);
        if (prey == null) {
            return false;
        }
        level.sendParticles(ParticleTypes.POOF, prey.getX(), prey.getY() + prey.getBbHeight() / 2, prey.getZ(), CRUMBS,
                CRUMB_SPREAD, CRUMB_SPREAD, CRUMB_SPREAD, 0.0);
        prey.discard();
        level.setBlock(pos, state.setValue(DIGESTING, true), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, FemboySounds.FLYTRAP_SNAP.get(), SoundSource.BLOCKS, SNAP_VOLUME, 1.0F);
        level.scheduleTick(pos, state.getBlock(), FemboyConfig.common().plants().flytrapDigestTicks());
        Vec3 center = Vec3.atCenterOf(pos);
        for (ServerPlayer player : level.getPlayers(player -> player.distanceToSqr(center) <= WITNESS_RANGE * WITNESS_RANGE)) {
            FemboyTriggers.fire(player, FemboyTriggers.FLYTRAP_SNACK);
        }
        return true;
    }

    /** Opens the jaws again ({@code next} is the state after the meal, grown or not) and goes back to hunting. */
    static void finishDigesting(ServerLevel level, BlockPos pos, BlockState next) {
        level.setBlock(pos, next.setValue(DIGESTING, false), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, FemboySounds.FLYTRAP_GULP.get(), SoundSource.BLOCKS, GULP_VOLUME, 1.0F);
        level.scheduleTick(pos, next.getBlock(), FemboyConfig.common().plants().flytrapCheckInterval());
    }
}
