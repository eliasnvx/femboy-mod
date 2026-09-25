package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** "Wardrobe slots" screen: cosmetic slots in rows of nine above the player inventory. */
public final class CosmeticsMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int SLOTS_LEFT = 8;
    public static final int SLOTS_TOP = 18;
    /** Gap between the cosmetic rows and the player inventory, as in chest menus. */
    public static final int INVENTORY_GAP = 13;

    private final CosmeticsContainer cosmetics;
    private final int cosmeticSlotCount;
    private final int rows;

    public CosmeticsMenu(int containerId, Inventory inventory) {
        super(FemboyMenus.COSMETICS.get(), containerId);
        Player player = inventory.player;
        this.cosmetics = new CosmeticsContainer(player);
        this.cosmeticSlotCount = cosmetics.getContainerSize();
        this.rows = Math.max(1, (cosmeticSlotCount + COLUMNS - 1) / COLUMNS);

        for (int i = 0; i < cosmeticSlotCount; i++) {
            addSlot(new CosmeticSlot(cosmetics, player, i,
                    SLOTS_LEFT + (i % COLUMNS) * SLOT_SIZE, SLOTS_TOP + (i / COLUMNS) * SLOT_SIZE));
        }
        addStandardInventorySlots(inventory, SLOTS_LEFT, inventoryTop());
    }

    public int rows() {
        return rows;
    }

    public int inventoryTop() {
        return SLOTS_TOP + rows * SLOT_SIZE + INVENTORY_GAP;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < cosmeticSlotCount) {
            // cosmetic -> player inventory
            if (!moveItemStackTo(stack, cosmeticSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // player inventory -> the matching empty cosmetic slot
            Identifier target = CosmeticsManager.slotOf(stack);
            int targetIndex = target == null ? -1 : CosmeticsManager.orderedSlots().indexOf(target);
            if (targetIndex < 0 || !moveItemStackTo(stack, targetIndex, targetIndex + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return cosmetics.stillValid(player);
    }
}
