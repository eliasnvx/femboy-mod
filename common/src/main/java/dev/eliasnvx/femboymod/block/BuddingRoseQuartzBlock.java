package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Budding Rose Quartz (quartz geodes): like budding amethyst, slowly grows rose quartz buds on its free sides,
 * small → medium → large → cluster. Breaks when mined or pushed; can't be obtained.
 */
public class BuddingRoseQuartzBlock extends Block {

    /** One growth attempt per this many random ticks, as for amethyst. */
    public static final int GROWTH_CHANCE = 5;
    private static final Direction[] DIRECTIONS = Direction.values();

    public BuddingRoseQuartzBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_CHANCE) == 0) {
            grow(level, pos, DIRECTIONS[random.nextInt(DIRECTIONS.length)]);
        }
    }

    /** Grows the bud on one side a stage further (also used by GameTests). */
    public static void grow(ServerLevel level, BlockPos pos, Direction side) {
        BlockPos target = pos.relative(side);
        BlockState current = level.getBlockState(target);
        Block next = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(current)) {
            next = FemboyBlocks.SMALL_ROSE_QUARTZ_BUD.get();
        } else if (isBudFacing(current, FemboyBlocks.SMALL_ROSE_QUARTZ_BUD.get(), side)) {
            next = FemboyBlocks.MEDIUM_ROSE_QUARTZ_BUD.get();
        } else if (isBudFacing(current, FemboyBlocks.MEDIUM_ROSE_QUARTZ_BUD.get(), side)) {
            next = FemboyBlocks.LARGE_ROSE_QUARTZ_BUD.get();
        } else if (isBudFacing(current, FemboyBlocks.LARGE_ROSE_QUARTZ_BUD.get(), side)) {
            next = FemboyBlocks.ROSE_QUARTZ_CLUSTER.get();
        }
        if (next != null) {
            level.setBlockAndUpdate(target, next.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, side)
                    .setValue(AmethystClusterBlock.WATERLOGGED, current.getFluidState().getType() == Fluids.WATER));
        }
    }

    private static boolean isBudFacing(BlockState state, Block bud, Direction side) {
        return state.is(bud) && state.getValue(AmethystClusterBlock.FACING) == side;
    }
}
