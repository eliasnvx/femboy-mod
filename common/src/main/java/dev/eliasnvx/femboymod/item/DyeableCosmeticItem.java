package dev.eliasnvx.femboymod.item;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

/**
 * A cosmetic that vanilla can dye. 1.21.1 marked these with {@code #minecraft:dyeable} and kept the color in
 * {@code minecraft:dyed_color}; on 1.20.1 the armor dye recipe and the cauldron's {@code DYED_ITEM} wash only
 * accept {@link DyeableLeatherItem}s (color in {@code display.color}).
 */
public class DyeableCosmeticItem extends Item implements DyeableLeatherItem {

    public DyeableCosmeticItem(Properties properties) {
        super(properties);
    }
}
