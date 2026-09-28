package dev.eliasnvx.femboymod.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Items in numbered slots, stored on an item (backpack contents, charms). Stands in for 1.21's
 * {@code ItemContainerContents}: immutable, holds copies, and keeps empty slots so positions survive.
 */
public final class ItemList {

    public static final ItemList EMPTY = new ItemList(List.of());

    private record SlotStack(int slot, ItemStack stack) {
        static final Codec<SlotStack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 255).fieldOf("Slot").forGetter(SlotStack::slot),
                ItemStack.CODEC.fieldOf("Item").forGetter(SlotStack::stack)
        ).apply(i, SlotStack::new));
    }

    /** Only non-empty slots are written, each with its index. */
    public static final Codec<ItemList> CODEC = SlotStack.CODEC.listOf().xmap(ItemList::fromSlots, ItemList::toSlots);

    private final List<ItemStack> items;

    private ItemList(List<ItemStack> items) {
        this.items = items;
    }

    /** @return a list holding copies of {@code items} (empty stacks keep their slot) */
    public static ItemList fromItems(List<ItemStack> items) {
        if (items.stream().allMatch(ItemStack::isEmpty)) {
            return EMPTY;
        }
        List<ItemStack> copy = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            copy.add(stack.copy());
        }
        return new ItemList(List.copyOf(copy));
    }

    /** @return number of slots, including trailing empty ones that were stored */
    public int size() {
        return items.size();
    }

    /** @return a copy of the stack in {@code slot}, or empty */
    public ItemStack get(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY;
    }

    /** @return copies of all stacks, empty slots included */
    public Stream<ItemStack> stream() {
        return items.stream().map(ItemStack::copy);
    }

    /** @return copies of the non-empty stacks */
    public Stream<ItemStack> nonEmptyStream() {
        return items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy);
    }

    /** @return copies of the non-empty stacks */
    public List<ItemStack> nonEmptyItems() {
        return nonEmptyStream().toList();
    }

    /** Copies the stacks into {@code target} slot by slot; slots beyond this list become empty. */
    public void copyInto(NonNullList<ItemStack> target) {
        for (int slot = 0; slot < target.size(); slot++) {
            target.set(slot, get(slot));
        }
    }

    private static ItemList fromSlots(List<SlotStack> slots) {
        int size = slots.stream().mapToInt(slot -> slot.slot() + 1).max().orElse(0);
        List<ItemStack> items = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
        for (SlotStack slot : slots) {
            items.set(slot.slot(), slot.stack());
        }
        return size == 0 ? EMPTY : new ItemList(List.copyOf(items));
    }

    private List<SlotStack> toSlots() {
        List<SlotStack> slots = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                slots.add(new SlotStack(slot, items.get(slot)));
            }
        }
        return slots;
    }
}
