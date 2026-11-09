package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Potted Grass (0.3.0): a desk decoration to touch grass without leaving your room. Made like any potted plant (grass
 * into a flower pot). An empty hand touches it ({@link dev.eliasnvx.femboymod.social.TouchGrass}, through the
 * {@code #femboymod:touchable_grass} tag) instead of pulling the grass out; break the pot to get both back.
 *
 * <p>1.20.1 has one {@code use} for every click (1.21.1 splits it into {@code useItemOn} and {@code useWithoutItem}), and
 * the vanilla pot pulls its plant out on a click with anything that isn't a pottable plant: here nothing comes out.
 */
public class PottedGrassBlock extends FlowerPotBlock {

    public PottedGrassBlock(Block grass, Properties properties) {
        super(grass, properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS; // the grass stays in the pot; touching it is handled before this
    }
}
