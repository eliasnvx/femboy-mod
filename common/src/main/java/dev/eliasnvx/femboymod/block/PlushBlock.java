package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.registry.FemboySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Plushies: decorative; squish-and-bounce on placement and right-click, squeaking on interaction. */
public class PlushBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private final Map<Direction, VoxelShape> shapes;
    private final @Nullable SimpleParticleType extraParticle;
    private static final float MIN_PITCH = 1.3F;
    private static final float PITCH_RANGE = 0.4F;
    private static final float PLACE_VOLUME = 0.35F;
    private static final float PLACE_MIN_PITCH = 1.05F;
    private static final float PLACE_PITCH_RANGE = 0.2F;

    /** @param northShape outline with the face pointing north (the model's orientation) */
    public PlushBlock(Properties properties, VoxelShape northShape) {
        this(properties, northShape, null);
    }

    /** @param extraParticle optional themed particle emitted alongside hearts on right-click (e.g. portal sparks) */
    public PlushBlock(Properties properties, VoxelShape northShape, @Nullable SimpleParticleType extraParticle) {
        super(properties);
        this.shapes = BlockShapes.rotateHorizontal(northShape);
        this.extraParticle = extraParticle;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Optional themed particle emitted alongside hearts when squeaked. */
    public @Nullable SimpleParticleType extraParticle() {
        return extraParticle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * Vertical offset so plushies placed above partial surfaces (beds, slabs, carpets, rugs, pet beds)
     * rest directly on top of the surface instead of floating above it.
     */
    public static double restingYOffset(@Nullable BlockGetter level, BlockPos pos) {
        if (level == null) {
            return 0.0;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.isAir()) {
            return 0.0;
        }
        Block block = below.getBlock();
        if (block instanceof BedBlock
                || block instanceof SlabBlock
                || block instanceof CarpetBlock
                || block instanceof FluffyRugBlock
                || block instanceof PetBedBlock) {
            VoxelShape belowShape = below.getShape(level, belowPos);
            if (!belowShape.isEmpty()) {
                double topY = belowShape.max(Direction.Axis.Y);
                if (topY > 0.0 && topY < 1.0) {
                    return topY - 1.0;
                }
            }
        }
        return 0.0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape base = shapes.get(state.getValue(FACING));
        double yOffset = restingYOffset(level, pos);
        return yOffset == 0.0 ? base : base.move(0.0, yOffset, 0.0);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        return !below.isAir() && !below.getShape(level, belowPos).isEmpty();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos.below()).getBlock() instanceof BedBlock) {
            return Shapes.empty();
        }
        return getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlushBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.blockEvent(pos, this, PlushBlockEntity.EVENT_PLACE, 0);
            float pitch = PLACE_MIN_PITCH + level.getRandom().nextFloat() * PLACE_PITCH_RANGE;
            level.playSound(null, pos, FemboySounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, PLACE_VOLUME, pitch);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            level.blockEvent(pos, this, PlushBlockEntity.EVENT_CLICK, 0);
            float pitch = MIN_PITCH + level.getRandom().nextFloat() * PITCH_RANGE;
            level.playSound(null, pos, FemboySounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, 1.0F, pitch);
            double yOffset = restingYOffset(level, pos);
            serverLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8 + yOffset, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.0);
            if (extraParticle != null) {
                serverLevel.sendParticles(extraParticle, pos.getX() + 0.5, pos.getY() + 0.65 + yOffset, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
