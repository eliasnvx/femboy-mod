package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Catnip (0.3.0): a wild mint of plains and meadows, plantable and pottable. Cats that smell it (or get fed a sprig)
 * get the zoomies ({@link dev.eliasnvx.femboymod.entity.Catnip}). Bone meal spreads it to a free spot next to it, like
 * a firefly bush, so it can be farmed.
 */
public class CatnipBlock extends VegetationBlock implements BonemealableBlock {

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);

    public CatnipBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos)); // planted off-center like flowers
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return BonemealableBlock.hasSpreadableNeighbourPos(level, pos, state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        BonemealableBlock.findSpreadableNeighbourPos(level, pos, state)
                .ifPresent(spot -> level.setBlockAndUpdate(spot, defaultBlockState()));
    }
}
