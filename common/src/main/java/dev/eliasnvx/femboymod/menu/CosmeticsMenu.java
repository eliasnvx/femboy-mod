package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.profile.StyleCoupons;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyMenus;
import dev.eliasnvx.femboymod.wardrobe.Outfits;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wardrobe: cosmetic slots arranged around a player doll (head items on the left, body and legs on the
 * right), extra slots (addons) in a row under the doll, player inventory below.
 */
public final class CosmeticsMenu extends AbstractContainerMenu {

    public static final int SLOT_SIZE = 18;
    public static final int WIDTH = 176;
    public static final int TOP = 18;
    public static final int LEFT_X = 8;
    public static final int RIGHT_X = WIDTH - 8 - 16;
    public static final int COLUMN_SLOTS = 5;
    /** The doll's window and the info box between the two slot columns. */
    public static final int DOLL_X = 28;
    public static final int DOLL_W = 62;
    public static final int INFO_X = DOLL_X + DOLL_W + 2;
    public static final int INFO_W = RIGHT_X - 4 - INFO_X;
    public static final int SIDE_H = COLUMN_SLOTS * SLOT_SIZE;
    public static final int EXTRA_GAP = 4;
    public static final int MAX_EXTRA_PER_ROW = 6;
    /** Label row plus gap between the wardrobe part and the player inventory, as in vanilla. */
    public static final int INVENTORY_GAP = 14;
    public static final int INVENTORY_PART = 83;

    /** Slots shown in the left column (top to bottom); everything else goes right, then to the extra row. */
    private static final List<Identifier> LEFT = List.of(FemboySlots.HEAD_ACCESSORY, FemboySlots.FACE, FemboySlots.NECK,
            FemboySlots.OUTFIT_TOP, FemboySlots.HANDS);
    private static final List<Identifier> RIGHT = List.of(FemboySlots.BACK, FemboySlots.TAIL, FemboySlots.OUTFIT_BOTTOM,
            FemboySlots.WAIST, FemboySlots.LEGS_OVERLAY);

    public static final int APPLY_BUTTON = 0;
    public static final int SAVE_BUTTON = WardrobePresets.COUNT;
    /** Trade Style Points for one Style Coupon. */
    public static final int REDEEM_BUTTON = 2 * WardrobePresets.COUNT;
    /** Presets applied from the outfit screen look only in the player's inventory. */
    private static final Container NO_WARDROBE = new SimpleContainer(0);

    private final CosmeticsContainer cosmetics;
    private final int cosmeticSlotCount;
    private final int extraRows;

    public CosmeticsMenu(int containerId, Inventory inventory) {
        super(FemboyMenus.COSMETICS.get(), containerId);
        Player player = inventory.player;
        this.cosmetics = new CosmeticsContainer(player);
        this.cosmeticSlotCount = cosmetics.getContainerSize();

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();
        List<Integer> extra = new ArrayList<>();
        for (int i = 0; i < cosmeticSlotCount; i++) {
            Identifier id = cosmetics.slotId(i);
            (LEFT.contains(id) ? left : RIGHT.contains(id) ? right : extra).add(i);
        }
        left.sort(Comparator.comparingInt(i -> LEFT.indexOf(cosmetics.slotId(i))));
        right.sort(Comparator.comparingInt(i -> RIGHT.indexOf(cosmetics.slotId(i))));
        while (right.size() < COLUMN_SLOTS && !extra.isEmpty()) {
            right.add(extra.removeFirst()); // first addon slot fills the free spot on the right
        }
        this.extraRows = (extra.size() + MAX_EXTRA_PER_ROW - 1) / MAX_EXTRA_PER_ROW;

        int[] xs = new int[cosmeticSlotCount];
        int[] ys = new int[cosmeticSlotCount];
        for (int row = 0; row < left.size(); row++) {
            xs[left.get(row)] = LEFT_X;
            ys[left.get(row)] = TOP + row * SLOT_SIZE;
        }
        for (int row = 0; row < right.size(); row++) {
            xs[right.get(row)] = RIGHT_X;
            ys[right.get(row)] = TOP + row * SLOT_SIZE;
        }
        for (int n = 0; n < extra.size(); n++) {
            xs[extra.get(n)] = DOLL_X + 1 + (n % MAX_EXTRA_PER_ROW) * SLOT_SIZE;
            ys[extra.get(n)] = TOP + SIDE_H + EXTRA_GAP + (n / MAX_EXTRA_PER_ROW) * SLOT_SIZE;
        }
        for (int i = 0; i < cosmeticSlotCount; i++) {
            addSlot(new CosmeticSlot(cosmetics, player, i, xs[i], ys[i]));
        }
        addStandardInventorySlots(inventory, LEFT_X, inventoryTop());
    }

    public int inventoryTop() {
        return TOP + SIDE_H + (extraRows > 0 ? EXTRA_GAP + extraRows * SLOT_SIZE : 0) + INVENTORY_GAP;
    }

    public int height() {
        return inventoryTop() + INVENTORY_PART;
    }

    public int cosmeticSlotCount() {
        return cosmeticSlotCount;
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

    /** Buttons 0..4 put on preset N (items come from the inventory), 5..9 save the current outfit as preset N. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer && id == REDEEM_BUTTON) {
            StyleCoupons.redeem(serverPlayer);
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer && id >= 0 && id < 2 * WardrobePresets.COUNT) {
            if (id < SAVE_BUTTON) {
                Outfits.apply(serverPlayer, NO_WARDROBE, id - APPLY_BUTTON);
            } else {
                Outfits.save(serverPlayer, id - SAVE_BUTTON);
            }
            broadcastChanges();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return cosmetics.stillValid(player);
    }
}
