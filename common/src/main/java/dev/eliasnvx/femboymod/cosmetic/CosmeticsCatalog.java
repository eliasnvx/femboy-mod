package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** Every wearable item (has a default {@code femboymod:cosmetic} value), for the collection counter. */
public final class CosmeticsCatalog {

    private static int size = -1;

    private CosmeticsCatalog() {
    }

    /** Number of wearable items; addon items included. Computed once, items don't change after startup. */
    public static int size() {
        if (size < 0) {
            int count = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                if (FemboyComponents.COSMETIC.getDefault(item) != null) {
                    count++;
                }
            }
            size = count;
        }
        return size;
    }
}
