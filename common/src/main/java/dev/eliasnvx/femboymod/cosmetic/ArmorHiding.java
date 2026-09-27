package dev.eliasnvx.femboymod.cosmetic;

import net.minecraft.world.entity.player.Player;
import java.util.WeakHashMap;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Which vanilla armor pieces are drawn on a player wearing cosmetics. Visual only: the armor still protects.
 * The server decides whether hiding is allowed (game rule {@code femboymod:allow_hidden_armor}) and sends the flag
 * with every cosmetics sync.
 */
public final class ArmorHiding {

    public static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /** Client copy of the server's game rule; the integrated server writes the same value. */
    private static volatile boolean allowedOnClient = true;

    private ArmorHiding() {
    }

    public static boolean allowedOnClient() {
        return allowedOnClient;
    }

    public static void setAllowedOnClient(boolean allowed) {
        allowedOnClient = allowed;
    }

    /** Whether {@code armorSlot} is hidden for this outfit. */
    public static boolean isHidden(CosmeticInventory inventory, EquipmentSlot armorSlot, boolean allowed) {
        if (!allowed) {
            return false;
        }
        return switch (inventory.armorVisibility(armorSlot)) {
            case HIDE -> true;
            case SHOW -> false;
            case AUTO -> coveredByOutfit(inventory, armorSlot);
        };
    }

    /** A worn, visible cosmetic whose slot covers this armor slot. */
    public static boolean coveredByOutfit(CosmeticInventory inventory, EquipmentSlot armorSlot) {
        for (Map.Entry<ResourceLocation, ItemStack> entry : inventory.all().entrySet()) {
            if (!inventory.isHidden(entry.getKey()) && CosmeticsManager.knownSlotType(entry.getKey()).map(CosmeticSlotType::coversArmor)
                    .map(covers -> covers.contains(armorSlot)).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /** Hidden armor as a bit mask (bit = index in {@link #ARMOR_SLOTS}). */
    public static int mask(CosmeticInventory inventory, boolean allowed) {
        int mask = 0;
        for (int i = 0; i < ARMOR_SLOTS.size(); i++) {
            if (isHidden(inventory, ARMOR_SLOTS.get(i), allowed)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private record Cached(CosmeticInventory inventory, boolean allowed, int mask) {
    }

    private static final Map<Player, Cached> CACHE = new WeakHashMap<>();

    /** {@link #mask} cached per player until the outfit or the rule changes (called every frame while rendering). */
    public static synchronized int cachedMask(Player player) {
        CosmeticInventory inventory = CosmeticsManager.get(player);
        boolean allowed = allowedOnClient;
        Cached cached = CACHE.get(player);
        if (cached == null || cached.inventory() != inventory || cached.allowed() != allowed) {
            cached = new Cached(inventory, allowed, mask(inventory, allowed));
            CACHE.put(player, cached);
        }
        return cached.mask();
    }

    /** Elytra and other gliders stay visible even with the chest piece hidden. */
    public static ItemStack visible(ItemStack worn, boolean hidden) {
        return hidden && !(worn.getItem() instanceof ElytraItem) ? ItemStack.EMPTY : worn;
    }
}
