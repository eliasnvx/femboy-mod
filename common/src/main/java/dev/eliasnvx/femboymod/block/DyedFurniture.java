package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Furniture in the 16 dye colors (bean bag, fluffy rug, pet bed, lava lamp): right-click with a dye to recolor it in
 * place. The color travels with the item (loot {@code copy_state}, the item's {@code BlockStateTag} NBT).
 */
public final class DyedFurniture {

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final DyeColor DEFAULT_COLOR = DyeColor.PINK;

    private DyedFurniture() {
    }

    /**
     * Recolors the block with the dye in {@code stack} and uses up one dye (not in creative). Anything that isn't a
     * dye, or the color it already has, gives {@link InteractionResult#PASS}: the block goes on with its empty-hand
     * use (1.21.1: {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}; 1.20.1 has one {@code use} for both).
     */
    public static InteractionResult dye(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        // 1.20.1 has no dye component (26.3: DataComponents.DYE): the dye is the item
        if (!(stack.getItem() instanceof DyeItem dyeItem) || state.getValue(COLOR) == dyeItem.getDyeColor()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(COLOR, dyeItem.getDyeColor()), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
