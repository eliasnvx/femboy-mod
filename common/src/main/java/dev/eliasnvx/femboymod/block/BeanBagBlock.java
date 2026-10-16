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

/**
 * Bean Bag: a squashy seat in any dye color. Right-click to sit (same slow heal and setup rating as the Gamer Chair),
 * right-click with a dye to recolor it.
 */
public class BeanBagBlock extends HorizontalDirectionalBlock implements SeatBlock {

    /** Sunk into the bag; the model's dent is at 5 px. */
    private static final double SEAT_HEIGHT = 0.15;
    /** The bag with the back hump to the south (model faces north). */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0),
            Block.box(2.0, 7.0, 10.0, 14.0, 13.0, 15.0));
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public BeanBagBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(DyedFurniture.COLOR, DyedFurniture.DEFAULT_COLOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DyedFurniture.COLOR);
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
    public double seatHeight() {
        return SEAT_HEIGHT;
    }

    /** A dye recolors it; otherwise (main hand, like 1.21.1's {@code useWithoutItem}) the player sits down. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult dyed = DyedFurniture.dye(player.getItemInHand(hand), state, level, pos, player);
        if (dyed != InteractionResult.PASS || hand != InteractionHand.MAIN_HAND) {
            return dyed;
        }
        if (player.isSecondaryUseActive() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            Seat.sit(serverLevel, pos, player, SEAT_HEIGHT);
        }
        return InteractionResult.SUCCESS;
    }
}
