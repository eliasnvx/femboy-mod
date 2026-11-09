package dev.eliasnvx.femboymod.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Catnip (0.3.0): a wild mint of plains and meadows, plantable and pottable. Cats that smell it (or get fed a sprig)
 * get the zoomies ({@link dev.eliasnvx.femboymod.entity.Catnip}). Bone meal spreads it to a free spot next to it, like
 * a firefly bush, so it can be farmed.
 *
 * <p>1.20.1 has no {@code VegetationBlock} (it's {@link BushBlock}), blocks have no codecs, and
 * {@link BonemealableBlock} has no neighbour-spread helpers: the spread below does what 26.3's
 * {@code BonemealableBlock.findSpreadableNeighbourPos} does.
 */
public class CatnipBlock extends BushBlock implements BonemealableBlock {

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);

    public CatnipBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos); // planted off-center like flowers
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return spreadSpot(Direction.Plane.HORIZONTAL.stream().toList(), level, pos, state).isPresent();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        spreadSpot(Direction.Plane.HORIZONTAL.shuffledCopy(random), level, pos, state)
                .ifPresent(spot -> level.setBlockAndUpdate(spot, defaultBlockState()));
    }

    /** The first free neighbour (in {@code directions} order) where catnip can grow. */
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
