package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the cosmetic panel next to the vanilla inventory. The panel's slots are not menu slots
 * (the player's inventory menu stays vanilla for compatibility); clicks arrive as packets and are applied
 * here to the real stacks: the menu's carried stack and the cosmetic inventory. Nothing is ever copied
 * without the original being removed in the same step (dupe tests: {@code PanelGameTests}).
 */
public final class CosmeticPanelActions {

    private CosmeticPanelActions() {
    }

    /**
     * @param quickMove shift-click: move the worn item into the player's inventory
     * @return whether anything changed
     */
    public static boolean click(ServerPlayer player, Identifier slot, boolean quickMove) {
        AbstractContainerMenu menu = player.inventoryMenu;
        if (player.containerMenu != menu || !player.isAlive() || player.isSpectator()
                || !CosmeticsManager.orderedSlots().contains(slot) || FemboyConfig.common().disabledSlots().contains(slot)) {
            return false;
        }
        ItemStack worn = CosmeticsManager.get(player).get(slot);
        ItemStack carried = menu.getCarried();
        boolean changed;
        if (quickMove) {
            changed = moveToInventory(player, slot, worn);
        } else if (carried.isEmpty()) {
            changed = !worn.isEmpty();
            if (changed) {
                CosmeticsManager.set(player, slot, ItemStack.EMPTY);
                menu.setCarried(worn);
            }
        } else {
            changed = equipCarried(player, menu, slot, worn, carried);
        }
        if (changed) {
            menu.broadcastChanges();
        }
        return changed;
    }

    private static boolean moveToInventory(ServerPlayer player, Identifier slot, ItemStack worn) {
        if (worn.isEmpty()) {
            return false;
        }
        // Check room first: in creative, Inventory#add reports success on a full inventory and deletes the stack.
        if (player.getInventory().getSlotWithRemainingSpace(worn) < 0 && player.getInventory().getFreeSlot() < 0) {
            return false;
        }
        ItemStack moving = worn.copy();
        if (!player.getInventory().add(moving) || !moving.isEmpty()) {
            return false;
        }
        CosmeticsManager.set(player, slot, ItemStack.EMPTY);
        return true;
    }

    private static boolean equipCarried(ServerPlayer player, AbstractContainerMenu menu, Identifier slot, ItemStack worn,
                                        ItemStack carried) {
        if (!CosmeticsManager.canEquip(player, slot, carried)) {
            return false;
        }
        ItemStack placing = carried.split(1);
        CosmeticsManager.set(player, slot, placing);
        if (!worn.isEmpty()) {
            if (carried.isEmpty()) {
                menu.setCarried(worn); // swap
            } else {
                player.getInventory().placeItemBackInInventory(worn, Prediction.SERVER_ONLY); // stackable addon item: rest stays carried
            }
        } else if (carried.isEmpty()) {
            menu.setCarried(ItemStack.EMPTY);
        }
        return true;
    }
}
