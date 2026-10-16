package dev.eliasnvx.femboymod.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Fairy Lights: a garland of small bulbs hung on a wall, sagging from edge to edge so neighbours join into one string.
 * Right-click cycles the bulbs: warm white, pink, pastel colors. Steady light, no blinking (SPEC §1.1).
 */
public class FairyLightsBlock extends HorizontalDirectionalBlock {

    public static final int MODES = 3;
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, MODES - 1);
    public static final int LIGHT = 8;
    private static final float MIN_PITCH = 1.4F;
    private static final float PITCH_STEP = 0.15F;
    /** The string hangs on the wall behind it (model: facing north, wall to the south). */
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(Block.box(0.0, 7.0, 14.0, 16.0, 16.0, 16.0));

    public FairyLightsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /** Hangs on the clicked wall; on a floor or ceiling it looks for a wall the player faces. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                  BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Main hand only, like 1.21.1's {@code useWithoutItem} (any item in hand just cycles the bulbs). */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            int mode = (state.getValue(MODE) + 1) % MODES;
            level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, /* 1.20.1 has no copper bulb sound */ SoundSource.BLOCKS,
                    0.3F, MIN_PITCH + mode * PITCH_STEP);
        }
        return InteractionResult.SUCCESS;
    }
}
