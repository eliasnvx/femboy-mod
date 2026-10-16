package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 3x3 storage of the Laundry Basket and the Mini Fridge. Slots take only what the container accepts
 * ({@link Container#canPlaceItem}), so the menu, shift-click and hoppers agree. The client copy filters by the same tag.
 */
public final class FurnitureStorageMenu extends AbstractContainerMenu {

    public static final int SIZE = 9;
    private static final int COLUMNS = 3;
    private static final int GRID_X = 62;
    private static final int GRID_Y = 17;
    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;

    private final Container container;

    /** Client constructor of the Laundry Basket menu. */
    public static FurnitureStorageMenu laundryBasket(int id, Inventory inventory) {
        return new FurnitureStorageMenu(FemboyMenus.LAUNDRY_BASKET.get(), id, inventory, filtered(FemboyTags.LAUNDRY));
    }

    /** Client constructor of the Mini Fridge menu. */
    public static FurnitureStorageMenu miniFridge(int id, Inventory inventory) {
        return new FurnitureStorageMenu(FemboyMenus.MINI_FRIDGE.get(), id, inventory, filtered(FemboyTags.FRIDGE_FOOD));
    }

    private static Container filtered(TagKey<Item> accepts) {
        return new SimpleContainer(SIZE) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return stack.is(accepts);
            }
        };
    }

    public FurnitureStorageMenu(MenuType<?> type, int id, Inventory inventory, Container container) {
        super(type, id);
        checkContainerSize(container, SIZE);
        this.container = container;
        container.startOpen(inventory.player);
        for (int i = 0; i < SIZE; i++) {
            addSlot(new Slot(container, i, GRID_X + (i % COLUMNS) * SLOT_SIZE, GRID_Y + (i / COLUMNS) * SLOT_SIZE) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    public Container container() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
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
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
