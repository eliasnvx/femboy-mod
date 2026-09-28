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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Neon sign (neon quartz): a glowing tube picture on a dark acrylic plate. Right-click cycles the design
 * (heart, cat, "btw"). Steady light, no flicker.
 */
public class NeonSignBlock extends HorizontalDirectionalBlock {


    public static final int DESIGNS = 3;
    public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, DESIGNS - 1);
    public static final int LIGHT = 10;
    /** Plate at the back of the block, the picture facing north (toward the player who placed it). */
    private static final VoxelShape NORTH_SHAPE = Block.box(1.0, 2.0, 14.0, 15.0, 14.0, 16.0);
    private final Map<Direction, VoxelShape> shapes = BlockShapes.rotateHorizontal(NORTH_SHAPE);

    public NeonSignBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DESIGN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DESIGN);
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
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(DESIGN, (state.getValue(DESIGN) + 1) % DESIGNS), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, /* 1.20.1 has no copper bulb sound */ SoundSource.BLOCKS, 0.4F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }
}
