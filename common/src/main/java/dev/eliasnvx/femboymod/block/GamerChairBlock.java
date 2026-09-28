package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.entity.Seat;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

/** Gamer Chair (SPEC v1.1): right-click to sit; sitting slowly heals (config {@code furniture}). */
public class GamerChairBlock extends HorizontalDirectionalBlock {


    /** Seat surface height; the model's cushion top is at 8 px. */
    private static final double SEAT_HEIGHT = 0.3;
    /** Base, seat and backrest with the back to the south (model faces north). */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0),
            Block.box(2.0, 6.0, 2.0, 14.0, 9.0, 14.0),
            Block.box(2.0, 9.0, 12.0, 14.0, 22.0, 15.0));
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public GamerChairBlock(Properties properties) {
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
        if (player.isSecondaryUseActive() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            Seat.sit(serverLevel, pos, player, SEAT_HEIGHT);
        }
        return InteractionResult.SUCCESS;
    }
}
