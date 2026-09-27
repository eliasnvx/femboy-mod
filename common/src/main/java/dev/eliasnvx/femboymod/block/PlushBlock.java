package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Plushies (SPEC §5.6): decorative; right-click squeaks. */
public class PlushBlock extends HorizontalDirectionalBlock {

    private final Map<Direction, VoxelShape> shapes;
    private final MapCodec<PlushBlock> codec;
    private static final float MIN_PITCH = 1.3F;
    private static final float PITCH_RANGE = 0.4F;

    /** @param northShape outline with the face pointing north (the model's orientation) */
    public PlushBlock(Properties properties, VoxelShape northShape) {
        super(properties);
        this.shapes = BlockShapes.rotateHorizontal(northShape);
        this.codec = simpleCodec(props -> new PlushBlock(props, northShape));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends PlushBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            float pitch = MIN_PITCH + level.getRandom().nextFloat() * PITCH_RANGE;
            level.playSound(null, pos, FemboySounds.PLUSH_SQUEAK.get(), SoundSource.BLOCKS, 1.0F, pitch);
            serverLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}
