package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticEquipEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticUnequipEvent;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/** Server-authoritative access to cosmetic slots. The client only mirrors synced state. */
public final class CosmeticsManager {

    private static SimpleApiRegistry<CosmeticSlotType> slotRegistry;
    private static List<Identifier> orderedSlots = List.of();

    private CosmeticsManager() {
    }

    /** Called once after all addons registered their slots and the registry is frozen. */
    public static void bake(SimpleApiRegistry<CosmeticSlotType> registry) {
        slotRegistry = registry;
        orderedSlots = registry.ids().stream()
                .sorted(Comparator.<Identifier>comparingInt(id -> registry.get(id).orElseThrow().sortOrder())
                        .thenComparing(Identifier::toString))
                .toList();
    }

    /** Slot ids in screen order. Identical on client and server. */
    public static List<Identifier> orderedSlots() {
        return orderedSlots;
    }

    public static CosmeticSlotType slotType(Identifier id) {
        return slotRegistry.get(id).orElseThrow(() -> new IllegalArgumentException("Unknown cosmetic slot " + id));
    }

    public static CosmeticInventory get(LivingEntity entity) {
        return entity instanceof Player player ? PlatformHelper.getCosmetics(player) : CosmeticInventory.EMPTY;
    }

    /** The slot an item is worn in, or null if the item is not a cosmetic or its slot is unknown. */
    public static Identifier slotOf(ItemStack stack) {
        Cosmetic cosmetic = stack.get(FemboyComponents.COSMETIC.get());
        if (cosmetic == null || slotRegistry.get(cosmetic.slot()).isEmpty()) {
            return null;
        }
        return cosmetic.slot();
    }

    /** Side-effect free check used by slots (both sides) and right-click equip. */
    public static boolean canEquip(LivingEntity entity, Identifier slot, ItemStack stack) {
        if (stack.isEmpty() || !slot.equals(slotOf(stack))) {
            return false;
        }
        return !FemboyMod.api().events().post(new CosmeticEquipEvent(entity, slot, stack)).isCancelled();
    }

    /**
     * Puts {@code stack} into the slot (ownership passes to the slot; pass a copy if you keep it).
     * On the server this posts events and syncs to the player and everyone tracking them.
     */
    public static void set(Player player, Identifier slot, ItemStack stack) {
        CosmeticInventory before = PlatformHelper.getCosmetics(player);
        ItemStack previous = before.get(slot);
        PlatformHelper.setCosmetics(player, before.with(slot, stack));

        if (player instanceof ServerPlayer serverPlayer) {
            if (!ItemStack.matches(previous, stack)) {
                if (!previous.isEmpty()) {
                    FemboyMod.api().events().post(new CosmeticUnequipEvent(player, slot, previous));
                }
                FemboyMod.api().events().post(new CosmeticChangedEvent(player, slot, previous, stack));
            }
            CosmeticsSyncPayload.sendToTrackingAndSelf(serverPlayer);
        }
    }

    /** Server: empties all slots and returns what was worn (used for death drops). */
    public static List<ItemStack> clear(ServerPlayer player) {
        List<ItemStack> removed = List.copyOf(get(player).all().values());
        for (Identifier slot : List.copyOf(get(player).all().keySet())) {
            set(player, slot, ItemStack.EMPTY);
        }
        return removed;
    }
}
