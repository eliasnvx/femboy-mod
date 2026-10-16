package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.registry.FemboyComponents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Every wearable item (has the {@code femboymod:cosmetic} component), for the collection counter and the Lookbook. */
public final class CosmeticsCatalog {

    private static int size = -1;
    private static List<Item> items;

    private CosmeticsCatalog() {
    }

    /** Number of wearable items; addon items included. Computed once, items don't change after startup. */
    public static int size() {
        if (size < 0) {
            int count = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.components().has(FemboyComponents.COSMETIC.get())) {
                    count++;
                }
            }
            size = count;
        }
        return size;
    }

    /**
     * Wearable items in Lookbook order: by slot (screen order), then registry order. Items of unknown slots are left
     * out. Computed once, after the slots are registered.
     */
    public static List<Item> items() {
        if (items == null) {
            List<ResourceLocation> slots = CosmeticsManager.orderedSlots();
            List<Item> found = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation slot = CosmeticsManager.slotOf(new ItemStack(item));
                if (slot != null) {
                    found.add(item);
                }
            }
            found.sort(Comparator.comparingInt(item -> slots.indexOf(CosmeticsManager.slotOf(new ItemStack(item)))));
            items = List.copyOf(found);
        }
        return items;
    }
}
