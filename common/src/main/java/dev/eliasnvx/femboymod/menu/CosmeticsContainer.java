package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Container view over a player's cosmetic attachment. Holds no items itself, so there is no
 * second copy that could drift from the stored state (dupe safety, see network-and-storage rules).
 */
public final class CosmeticsContainer implements Container {

    private final Player player;
    private final List<Identifier> slots;

    public CosmeticsContainer(Player player) {
        this.player = player;
        this.slots = CosmeticsManager.orderedSlots();
    }

    public Identifier slotId(int index) {
        return slots.get(index);
    }

    @Override
    public int getContainerSize() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        return CosmeticsManager.get(player).isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return CosmeticsManager.get(player).get(slots.get(index));
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack current = getItem(index);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = current.copy();
        ItemStack taken = remaining.split(amount);
        CosmeticsManager.set(player, slots.get(index), remaining);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack current = getItem(index);
        CosmeticsManager.set(player, slots.get(index), ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        CosmeticsManager.set(player, slots.get(index), stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        // Every mutation goes through CosmeticsManager.set, which stores and syncs.
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive();
    }

    @Override
    public void clearContent() {
        for (Identifier slot : slots) {
            CosmeticsManager.set(player, slot, ItemStack.EMPTY);
        }
    }
}
