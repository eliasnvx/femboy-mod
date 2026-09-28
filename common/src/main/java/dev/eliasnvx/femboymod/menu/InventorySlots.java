package dev.eliasnvx.femboymod.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

/** The player's 27 inventory slots and the hotbar, laid out like vanilla (1.20.1 has no addStandardInventorySlots). */
public final class InventorySlots {

    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    /** Gap between the main inventory and the hotbar. */
    public static final int HOTBAR_GAP = 4;

    private InventorySlots() {
    }

    /** Adds inventory rows starting at ({@code left}, {@code top}), then the hotbar below them. */
    public static void addStandard(Consumer<Slot> addSlot, Inventory inventory, int left, int top) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot.accept(new Slot(inventory, COLUMNS + row * COLUMNS + col, left + col * SLOT_SIZE, top + row * SLOT_SIZE));
            }
        }
        int hotbarTop = top + ROWS * SLOT_SIZE + HOTBAR_GAP;
        for (int col = 0; col < COLUMNS; col++) {
            addSlot.accept(new Slot(inventory, col, left + col * SLOT_SIZE, hotbarTop));
        }
    }
}
