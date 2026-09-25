package dev.eliasnvx.femboymod.wardrobe;

import dev.eliasnvx.femboymod.registry.FemboyMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Wardrobe: 27 storage slots + player inventory; buttons 0-4 put on a preset, 5-9 save one. */
public final class WardrobeMenu extends AbstractContainerMenu {

    public static final int ROWS = 3;
    public static final int SIZE = ROWS * 9;
    public static final int APPLY_BUTTON = 0;
    public static final int SAVE_BUTTON = WardrobePresets.COUNT;

    private final Container wardrobe;

    /** Client constructor. */
    public WardrobeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE));
    }

    public WardrobeMenu(int id, Inventory inventory, Container wardrobe) {
        super(FemboyMenus.WARDROBE.get(), id);
        checkContainerSize(wardrobe, SIZE);
        this.wardrobe = wardrobe;
        wardrobe.startOpen(inventory.player);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(wardrobe, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 18 + ROWS * 18 + 13);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer && id >= 0 && id < 2 * WardrobePresets.COUNT) {
            if (id < SAVE_BUTTON) {
                Outfits.apply(serverPlayer, wardrobe, id - APPLY_BUTTON);
            } else {
                Outfits.save(serverPlayer, id - SAVE_BUTTON);
            }
            broadcastChanges();
        }
        return true;
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
        return wardrobe.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        wardrobe.stopOpen(player);
    }
}
