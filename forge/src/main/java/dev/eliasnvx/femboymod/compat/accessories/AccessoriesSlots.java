package dev.eliasnvx.femboymod.compat.accessories;

import dev.eliasnvx.femboymod.compat.ExternalSlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Accessories (soft dependency): our cosmetics in Accessories slots count as worn. Which items fit which slot comes
 * from the data pack ({@code data/accessories/tags/item}). Loaded only with Accessories.
 */
public final class AccessoriesSlots {

    private AccessoriesSlots() {
    }

    public static void register() {
        ExternalSlots.register((player, action) -> {
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability != null) {
                for (SlotEntryReference entry : capability.getAllEquipped()) {
                    action.accept(entry.stack());
                }
            }
        });
    }

    /** Client: our cosmetic layer draws them; Accessories' default renderer would draw the item model on top. */
    public static void registerClient() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (CosmeticsManager.slotOf(new ItemStack(item)) != null) {
                AccessoriesRendererRegistry.registerNoRenderer(item);
            }
        }
    }
}
