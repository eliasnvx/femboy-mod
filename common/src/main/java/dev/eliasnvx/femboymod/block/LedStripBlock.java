package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * LED strip (SPEC v1.1) on any face. Right-click cycles the color: pink, purple, blue, white and a slow
 * rainbow (a smooth texture animation, never a strobe). Servers can turn the rainbow off
 * ({@code furniture.led_rainbow}); then only static colors are offered.
 */
public class LedStripBlock extends DirectionalBlock {

    public static final MapCodec<LedStripBlock> CODEC = simpleCodec(LedStripBlock::new);

    public static final int COLORS = 5;
    public static final int RAINBOW = COLORS - 1;
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, RAINBOW);
    public static final int LIGHT = 9;
    private static final float MIN_PITCH = 1.2F;
    private static final float PITCH_STEP = 0.1F;

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, Block.box(0.0, 0.0, 6.0, 16.0, 1.0, 10.0),
            Direction.DOWN, Block.box(0.0, 15.0, 6.0, 16.0, 16.0, 10.0),
            Direction.NORTH, Block.box(0.0, 6.0, 15.0, 16.0, 10.0, 16.0),
            Direction.SOUTH, Block.box(0.0, 6.0, 0.0, 16.0, 10.0, 1.0),
            Direction.EAST, Block.box(0.0, 6.0, 0.0, 1.0, 10.0, 16.0),
            Direction.WEST, Block.box(15.0, 6.0, 0.0, 16.0, 10.0, 16.0));

    public LedStripBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(COLOR, 0));
    }

    @Override
    protected MapCodec<LedStripBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOR);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Next color after {@code color}; skips the rainbow when {@code rainbowAllowed} is false. */
    public static int nextColor(int color, boolean rainbowAllowed) {
        int next = (color + 1) % COLORS;
        return next == RAINBOW && !rainbowAllowed ? 0 : next;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int color = nextColor(state.getValue(COLOR), FemboyConfig.common().furniture().ledRainbow());
            level.setBlock(pos, state.setValue(COLOR, color), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.4F, MIN_PITCH + color * PITCH_STEP);
        }
        return InteractionResult.SUCCESS;
    }
}
