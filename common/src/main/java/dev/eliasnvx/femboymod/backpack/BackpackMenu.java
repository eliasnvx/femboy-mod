package dev.eliasnvx.femboymod.backpack;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.cosmetic.CharmsChangedEvent;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/**
 * Backpack menu (SPEC §5.3). Dupe safety (network-and-storage rules):
 * <ul>
 *     <li>single source of truth: every change is written straight into the backpack item's component;</li>
 *     <li>{@link #stillValid}: the exact backpack stack (identity) must still be where it was opened from;</li>
 *     <li>the backpack's own inventory slot is locked (no pickup, throw, number-key or offhand swap);</li>
 *     <li>no backpacks (or anything that cannot go into containers, e.g. shulker boxes) inside.</li>
 * </ul>
 */
public final class BackpackMenu extends AbstractContainerMenu {

    public static final int SLOT_SIZE = 18;
    public static final int LEFT = 8;
    public static final int TOP = 18;
    public static final int INVENTORY_GAP = 13;
    public static final int HOTBAR_GAP = 4;
    /** Charm column sits on a side panel right of the 176 px wide main panel. */
    public static final int CHARM_X = 184;

    private final Player player;
    private final BackpackMenuData data;
    private final int bagSize;
    private final int lockedInventorySlot;
    private final ComponentContainer bag;
    private final ComponentContainer charms;
    private ItemStack backpack;

    /** Client side: Architectury 13 extended menus hand the extra data over as a raw buffer. */
    public BackpackMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, BackpackMenuData.STREAM_CODEC.decode(buf));
    }

    public BackpackMenu(int containerId, Inventory inventory, BackpackMenuData data) {
        super(FemboyMenus.BACKPACK.get(), containerId);
        this.player = inventory.player;
        this.data = data;
        this.backpack = data.resolve(player);
        this.bagSize = Math.max(1, data.rows()) * 9;
        this.lockedInventorySlot = data.worn() ? -1 : data.slot();
        this.bag = new ComponentContainer(DataComponents.CONTAINER, bagSize, false);
        this.charms = new ComponentContainer(FemboyComponents.CHARMS.get(), BackpackSpec.CHARM_SLOTS, true);

        for (int i = 0; i < bagSize; i++) {
            addSlot(new BagSlot(bag, i, LEFT + (i % 9) * SLOT_SIZE, TOP + (i / 9) * SLOT_SIZE));
        }
        for (int i = 0; i < BackpackSpec.CHARM_SLOTS; i++) {
            addSlot(new CharmSlot(charms, i, CHARM_X, TOP + i * SLOT_SIZE));
        }
        int inventoryTop = inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new LockableSlot(inventory, 9 + row * 9 + col, LEFT + col * SLOT_SIZE, inventoryTop + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new LockableSlot(inventory, col, LEFT + col * SLOT_SIZE, inventoryTop + 3 * SLOT_SIZE + HOTBAR_GAP));
        }
    }

    public int rows() {
        return bagSize / 9;
    }

    public int inventoryTop() {
        return TOP + rows() * SLOT_SIZE + INVENTORY_GAP;
    }

    public ItemStack backpack() {
        return backpack;
    }

    @Override
    public boolean stillValid(Player p) {
        return p == player && p.isAlive() && data.resolve(p) == backpack && BackpackMenus.spec(backpack) != null;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType input, Player p) {
        if (!stillValid(p)) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex) instanceof LockableSlot slot && slot.isLocked()) {
            return; // the open backpack itself: no pickup / throw / quick move / swap onto it
        }
        if (input == ClickType.SWAP && button == lockedInventorySlot) {
            return; // number key (or offhand) swap of the open backpack into a slot
        }
        super.clicked(slotIndex, button, input, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !stillValid(p)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int charmStart = bagSize;
        int inventoryStart = bagSize + BackpackSpec.CHARM_SLOTS;
        if (index < inventoryStart) {
            if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(FemboyTags.CHARMS) && moveItemStackTo(stack, charmStart, inventoryStart, false)) {
            // moved onto a free charm slot
        } else if (!moveItemStackTo(stack, 0, bagSize, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Container view over one ItemContainerContents component of the backpack. */
    private final class ComponentContainer implements Container {
        private final DataComponentType<ItemContainerContents> type;
        private final NonNullList<ItemStack> items;
        private final boolean isCharms;

        ComponentContainer(DataComponentType<ItemContainerContents> type, int size, boolean isCharms) {
            this.type = type;
            this.items = NonNullList.withSize(size, ItemStack.EMPTY);
            this.isCharms = isCharms;
            backpack.getOrDefault(type, ItemContainerContents.EMPTY).copyInto(items);
        }

        /** Writes the current slots into the backpack item immediately. */
        private void commit() {
            if (!stillValid(player)) {
                return; // never write into a backpack that is gone (would duplicate its contents)
            }
            backpack.set(type, ItemContainerContents.fromItems(items));
            if (isCharms && player instanceof ServerPlayer serverPlayer) {
                FemboyMod.api().events().post(new CharmsChangedEvent(player, backpack, List.copyOf(items)));
                if (data.worn()) {
                    // charms on the worn backpack change effects and what others see
                    WornEvaluator.invalidate(serverPlayer);
                    CosmeticsSyncPayload.sendToTrackingAndSelf(serverPlayer);
                }
            }
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
            if (!removed.isEmpty()) {
                commit();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = ContainerHelper.takeItem(items, slot);
            commit();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            commit();
        }

        @Override
        public void setChanged() {
            commit();
        }

        @Override
        public boolean stillValid(Player p) {
            return BackpackMenu.this.stillValid(p);
        }

        @Override
        public void clearContent() {
            items.clear();
            commit();
        }
    }

    private static final class BagSlot extends Slot {
        BagSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canStore(stack);
        }
    }

    /** No backpacks (and nothing that refuses to go into container items, like shulker boxes). */
    public static boolean canStore(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.has(FemboyComponents.BACKPACK.get());
    }

    private static final class CharmSlot extends Slot {
        CharmSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(FemboyTags.CHARMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private final class LockableSlot extends Slot {
        LockableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        boolean isLocked() {
            return getContainerSlot() == lockedInventorySlot;
        }

        @Override
        public boolean mayPickup(Player p) {
            return !isLocked() && super.mayPickup(p);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isLocked() && super.mayPlace(stack);
        }
    }
}
