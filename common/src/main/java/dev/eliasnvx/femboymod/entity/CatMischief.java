package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Cats knocking things off tables: which blocks qualify (a {@code #femboymod:cat_knockable} block on a surface, with
 * a free cell and a drop on one side and room for the cat on the other) and the push itself.
 *
 * <p>The push swats the block off as its normal drops (its loot table, like breaking it by hand), flung over the
 * edge: a potted flower comes off as the pot and the flower, three candles as three candles. A falling block would
 * look nicer, but when it can't land vanilla drops only the block's own item, which doesn't exist for potted plants
 * and is one candle for a stack of three. Blocks with block entities stay out of the tag.
 */
public final class CatMischief {

    /** How far the swatted items fly off the edge (blocks per tick) and how much they hop. */
    private static final double FLING = 0.18;
    private static final double HOP = 0.15;
    private static final float SWAT_VOLUME = 0.6F;
    /** Searched this far above and below the cat (a table is one block up). */
    private static final int SEARCH_HEIGHT = 2;

    /**
     * A block to knock and which way: the cat stands on the opposite side and pushes it away from itself.
     */
    public record Target(BlockPos block, Direction push) {

        /** Where the cat stands to push. */
        public BlockPos stand() {
            return block.relative(push.getOpposite());
        }
    }

    private CatMischief() {
    }

    /**
     * The game rules allow it: {@code femboymod:cat_mischief} and mob griefing for this cat (vanilla's rule; on NeoForge
     * its event, so claim mods can keep cats off protected tables).
     */
    public static boolean allowed(ServerLevel level, @Nullable Entity cat) {
        return level.getGameRules().getBoolean(FemboyGameRules.CAT_MISCHIEF) && PlatformHelper.canMobGrief(level, cat);
    }

    /**
     * Which way the block at {@code pos} can be pushed off its surface, or null: the next cell is free with a drop
     * below it, and the cell on the other side is free with something to stand on.
     */
    public static @Nullable Direction edgeDirection(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(FemboyTags.CAT_KNOCKABLE) || isFree(level, pos.below())) {
            return null;
        }
        for (Direction push : Direction.Plane.HORIZONTAL) {
            BlockPos over = pos.relative(push);
            BlockPos stand = pos.relative(push.getOpposite());
            if (isFree(level, over) && isFree(level, over.below()) && isFree(level, stand) && !isFree(level, stand.below())) {
                return push;
            }
        }
        return null;
    }

    /** The closest knockable block at an edge around {@code center}, or null. */
    public static @Nullable Target find(Level level, BlockPos center, int radius) {
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -SEARCH_HEIGHT, -radius),
                center.offset(radius, SEARCH_HEIGHT, radius))) {
            if (!level.getBlockState(pos).is(FemboyTags.CAT_KNOCKABLE)) {
                continue;
            }
            Direction push = edgeDirection(level, pos);
            double distance = pos.distSqr(center);
            if (push != null && distance < bestDistance) {
                best = new Target(pos.immutable(), push);
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The target is still there and still at an edge the same way. */
    public static boolean stillKnockable(Level level, Target target) {
        return edgeDirection(level, target.block()) == target.push();
    }

    /**
     * Swats the block off the edge: removes it (a block-destroyed game event by the cat, for sculk and logging mods)
     * and flings its drops over the edge. The audience (the player the cat was staring at) gets the advancement. Returns the spawned items (empty if there was nothing to knock).
     */
    public static List<ItemEntity> knock(ServerLevel level, Target target, @Nullable Entity cat, @Nullable Player audience) {
        BlockPos pos = target.block();
        BlockState state = level.getBlockState(pos);
        if (!state.is(FemboyTags.CAT_KNOCKABLE)) {
            return List.of();
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null);
        level.removeBlock(pos, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(cat, state));
        Direction push = target.push();
        List<ItemEntity> items = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + push.getStepX() * 0.5, pos.getY() + 0.25,
                    pos.getZ() + 0.5 + push.getStepZ() * 0.5, drop, push.getStepX() * FLING, HOP, push.getStepZ() * FLING);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
            items.add(item);
        }
        level.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.NEUTRAL, SWAT_VOLUME, 1.0F);
        if (audience instanceof ServerPlayer player) {
            FemboyTriggers.fire(player, FemboyTriggers.CAT_MISCHIEF);
        }
        return items;
    }

    /** Nothing solid and no fluid: an item or a cat can pass. */
    private static boolean isFree(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }
}
