package dev.eliasnvx.femboymod.wardrobe;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side outfit switching. Items are only ever moved (removed from one place, put into another),
 * never created: presets store what to look for, not items to hand out.
 */
public final class Outfits {

    private Outfits() {
    }

    /** Remembers what the player wears now as preset {@code index}. */
    public static void save(ServerPlayer player, int index) {
        Map<ResourceLocation, ItemStack> outfit = new HashMap<>();
        CosmeticsManager.get(player).all().forEach((slot, stack) -> outfit.put(slot, stack.copyWithCount(1)));
        PlatformHelper.setWardrobePresets(player, PlatformHelper.getWardrobePresets(player).with(index, outfit));
    }

    /**
     * Puts on preset {@code index}: for every slot, the saved item is looked up in the wardrobe, then in the
     * inventory. What was worn goes into the wardrobe (or the inventory, or is dropped). Slots whose item
     * cannot be found keep what is worn.
     *
     * @return number of slots that changed
     */
    public static int apply(ServerPlayer player, Container wardrobe, int index) {
        Map<ResourceLocation, ItemStack> outfit = PlatformHelper.getWardrobePresets(player).get(index).orElse(null);
        if (outfit == null) {
            return 0;
        }
        int changed = 0;
        for (ResourceLocation slot : CosmeticsManager.orderedSlots()) {
            ItemStack wanted = outfit.getOrDefault(slot, ItemStack.EMPTY);
            ItemStack worn = CosmeticsManager.get(player).get(slot);
            if (ItemStack.isSameItemSameComponents(wanted, worn) || (wanted.isEmpty() && worn.isEmpty())) {
                continue;
            }
            ItemStack found = wanted.isEmpty() ? ItemStack.EMPTY : take(wardrobe, wanted);
            if (!wanted.isEmpty() && found.isEmpty()) {
                found = take(player.getInventory(), wanted);
                if (found.isEmpty()) {
                    continue; // not available: keep what is worn
                }
            }
            ItemStack previous = worn.copy();
            CosmeticsManager.set(player, slot, found);
            if (!previous.isEmpty()) {
                store(player, wardrobe, previous);
            }
            changed++;
        }
        return changed;
    }

    private static ItemStack take(Container container, ItemStack wanted) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container instanceof Inventory && i >= Inventory.INVENTORY_SIZE) {
                break; // never take armor/offhand
            }
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                ItemStack taken = container.removeItem(i, 1);
                container.setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void store(ServerPlayer player, Container wardrobe, ItemStack stack) {
        for (int i = 0; i < wardrobe.getContainerSize() && !stack.isEmpty(); i++) {
            if (wardrobe.getItem(i).isEmpty() && wardrobe.canPlaceItem(i, stack)) {
                wardrobe.setItem(i, stack);
                wardrobe.setChanged();
                return;
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
}
