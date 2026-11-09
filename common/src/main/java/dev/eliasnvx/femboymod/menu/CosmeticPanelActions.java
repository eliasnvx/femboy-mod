package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

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
    public static boolean click(ServerPlayer player, ResourceLocation slot, boolean quickMove) {
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

    /**
     * Shift-click on a cosmetic in the inventory: wear it if its slot is empty. Runs on both sides; only the
     * server changes anything, the client just skips the vanilla move (the server's slot update follows).
     *
     * @return whether the vanilla quick move should be skipped
     */
    public static boolean quickEquip(InventoryMenu menu, Player player, int index) {
        ItemStack stack = menu.getSlot(index).getItem();
        ResourceLocation slot = CosmeticsManager.slotOf(stack);
        if (slot == null || !CosmeticsManager.get(player).get(slot).isEmpty() || !CosmeticsManager.canEquip(player, slot, stack)) {
            return false;
        }
        if (player instanceof ServerPlayer) {
            CosmeticsManager.set(player, slot, menu.getSlot(index).remove(1));
            menu.getSlot(index).setChanged();
        }
        return true;
    }

    /**
     * Creative inventory panel: the cursor is client-side, so the item arrives in the packet (creative only). 1.20.1
     * decodes packet stacks without validation, so this checks what vanilla's creative slot packet checks
     * ({@code ServerGamePacketListenerImpl#handleSetCreativeModeSlot}): creative mode, an item this world has, no
     * negative damage, at most a full stack, block entity data read back from a loaded block it points at. Plus ours:
     * not a spectator, alive, with only the inventory open.
     */
    public static boolean creativeSet(ServerPlayer player, ResourceLocation slot, ItemStack stack) {
        if (!player.gameMode.isCreative() || player.isSpectator() || !player.isAlive()
                || player.containerMenu != player.inventoryMenu || !CosmeticsManager.orderedSlots().contains(slot)
                || !stack.isItemEnabled(player.level().enabledFeatures())
                || !stack.isEmpty() && (stack.getDamageValue() < 0 || stack.getCount() > Item.MAX_STACK_SIZE)) {
            return false; // like vanilla's creative slot packet: creative, in the inventory, an item this world has
        }
        ItemStack single = stack.copyWithCount(Math.min(1, stack.getCount()));
        readBlockEntityBack(player, single);
        if (!single.isEmpty() && !CosmeticsManager.canEquip(player, slot, single)) {
            return false;
        }
        CosmeticsManager.set(player, slot, single);
        return true;
    }

    /** Vanilla's creative pick-block step: a stack naming a loaded block entity's position gets that block's real data. */
    private static void readBlockEntityBack(ServerPlayer player, ItemStack stack) {
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (stack.isEmpty() || data == null || !data.contains("x") || !data.contains("y") || !data.contains("z")) {
            return;
        }
        BlockPos pos = BlockEntity.getPosFromTag(data);
        if (player.level().isLoaded(pos)) {
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntity.saveToItem(stack);
            }
        }
    }

    private static boolean moveToInventory(ServerPlayer player, ResourceLocation slot, ItemStack worn) {
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

    private static boolean equipCarried(ServerPlayer player, AbstractContainerMenu menu, ResourceLocation slot, ItemStack worn,
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
                player.getInventory().placeItemBackInInventory(worn); // stackable addon item: rest stays carried
            }
        } else if (carried.isEmpty()) {
            menu.setCarried(ItemStack.EMPTY);
        }
        return true;
    }
}
