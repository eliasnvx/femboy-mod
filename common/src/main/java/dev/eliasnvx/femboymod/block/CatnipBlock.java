package dev.eliasnvx.femboymod.block;

import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Catnip: plantable from {@code catnip_seeds} (grows age 0..3) or full-grown. */
public class CatnipBlock extends BushBlock implements BonemealableBlock {

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {
            Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0),
            Block.box(4.0, 0.0, 4.0, 12.0, 6.0, 12.0),
            Block.box(3.5, 0.0, 3.5, 12.5, 8.0, 12.5),
            Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0),
    };

    public CatnipBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, MAX_AGE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getItemInHand().is(FemboyItems.CATNIP_SEEDS.get())) {
            return defaultBlockState().setValue(AGE, 0);
        }
        return defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPES[state.getValue(AGE)].move(offset.x, offset.y, offset.z);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && random.nextInt(5) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < MAX_AGE || spreadSpot(Direction.Plane.HORIZONTAL.stream().toList(), level, pos, state).isPresent();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            return;
        }
        spreadSpot(Direction.Plane.HORIZONTAL.shuffledCopy(random), level, pos, state)
                .ifPresent(spot -> level.setBlockAndUpdate(spot, defaultBlockState()));
    }

    private static Optional<BlockPos> spreadSpot(List<Direction> directions, LevelReader level, BlockPos pos, BlockState state) {
        for (Direction direction : directions) {
            BlockPos spot = pos.relative(direction);
            if (level.isEmptyBlock(spot) && state.canSurvive(level, spot)) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }
}
