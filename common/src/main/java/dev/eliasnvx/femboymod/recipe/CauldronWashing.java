package dev.eliasnvx.femboymod.recipe;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * Washing dye off cosmetics in a water cauldron. 26.3 does this through the {@code minecraft:cauldron_can_remove_dye}
 * item tag; 1.20.1 keys cauldron interactions by item, so every dyeable cosmetic item (ours and addons': a
 * {@link DyeableLeatherItem} with a default {@code femboymod:cosmetic} value) gets vanilla's
 * {@link CauldronInteraction#DYED_ITEM}. That interaction only removes the vanilla dye ({@code display.color}),
 * like 26.3; the colorway stays.
 */
final class CauldronWashing {

    private CauldronWashing() {
    }

    static void register() {
        // SETUP runs on both sides once every item is registered (client prediction needs the entry too)
        LifecycleEvent.SETUP.register(CauldronWashing::addWaterInteractions);
    }

    private static void addWaterInteractions() {
        Map<Item, CauldronInteraction> water = CauldronInteraction.WATER; // 1.20.1: a plain map, not an InteractionMap record
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof DyeableLeatherItem && FemboyComponents.COSMETIC.getDefault(item) != null) {
                water.putIfAbsent(item, CauldronInteraction.DYED_ITEM);
            }
        }
    }
}
