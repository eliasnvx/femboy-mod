package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.registry.FemboySounds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import org.jetbrains.annotations.Nullable;

/** Mechanical keyboard with a mouse: right-click to clack the keys. */
public class GamerKeyboardBlock extends HorizontalDirectionalBlock {


    private static final float MIN_PITCH = 0.92F;
    private static final float PITCH_RANGE = 0.16F;
    private static final float CLACK_VOLUME = 0.85F;
    private static final VoxelShape NORTH_SHAPE = Block.box(1.0, 0.0, 5.0, 15.0, 2.0, 11.0);
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public GamerKeyboardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            float pitch = MIN_PITCH + level.getRandom().nextFloat() * PITCH_RANGE;
            level.playSound(null, pos, FemboySounds.KEYBOARD_CLACK.get(), SoundSource.BLOCKS, CLACK_VOLUME, pitch);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 0.18, pos.getZ() + 0.5,
                    2, 0.25, 0.04, 0.12, 0.01);
        }
        return InteractionResult.SUCCESS;
    }
}
