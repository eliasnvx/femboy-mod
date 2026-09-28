package dev.eliasnvx.femboymod.api;

import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.item.ItemData;

/**
 * Item data added by Femboy Mod. Obtain via {@link FemboyApi#components()}.
 *
 * <p>On Minecraft 1.20.1 there are no data components: the values live in the item's NBT and are read and
 * written through {@link ItemData}.
 */
public interface FemboyDataComponents {

    /**
     * {@code femboymod:cosmetic} — see {@link Cosmetic}.
     *
     * @return accessor for the cosmetic definition of an item stack
     */
    ItemData<Cosmetic> cosmetic();

    /**
     * {@code femboymod:colorway} — see {@link Colorway}.
     *
     * @return accessor for the colorway of an item stack
     */
    ItemData<Colorway> colorway();
}
