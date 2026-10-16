package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.compat.ExternalSlots;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticEquipEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticUnequipEvent;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.List;

/** Server-authoritative access to cosmetic slots. The client only mirrors synced state. */
public final class CosmeticsManager {

    private static final float EQUIP_SOUND_VOLUME = 1.0F;
    private static final float EQUIP_SOUND_PITCH = 1.0F;

    private static SimpleApiRegistry<CosmeticSlotType> slotRegistry;
    private static List<ResourceLocation> orderedSlots = List.of();

    private CosmeticsManager() {
    }

    /** Called once after all addons registered their slots and the registry is frozen. */
    public static void bake(SimpleApiRegistry<CosmeticSlotType> registry) {
        slotRegistry = registry;
        orderedSlots = registry.ids().stream()
                .sorted(Comparator.<ResourceLocation>comparingInt(id -> registry.get(id).orElseThrow().sortOrder())
                        .thenComparing(ResourceLocation::toString))
                .toList();
    }

    /** Slot ids in screen order. Identical on client and server. */
    public static List<ResourceLocation> orderedSlots() {
        return orderedSlots;
    }

    /** The slot type, or empty for slots of removed addons (their items are kept but unknown). */
    public static java.util.Optional<CosmeticSlotType> knownSlotType(ResourceLocation id) {
        return slotRegistry.get(id);
    }

    public static CosmeticSlotType slotType(ResourceLocation id) {
        return slotRegistry.get(id).orElseThrow(() -> new IllegalArgumentException("Unknown cosmetic slot " + id));
    }

    public static CosmeticInventory get(LivingEntity entity) {
        return entity instanceof Player player ? PlatformHelper.getCosmetics(player) : CosmeticInventory.EMPTY;
    }

    /** What {@link #worn} merged last time for a player; kept while nothing changes so caches downstream hold. */
    private record WornCache(int tick, CosmeticInventory own, Set<ResourceLocation> extra, CosmeticInventory merged) {
    }

    private static final Map<Player, WornCache> WORN = new WeakHashMap<>();

    /**
     * The outfit as it is worn: {@link #get} plus cosmetics in other mods' slots ({@link ExternalSlots}) for slots
     * that are empty here. Backpacks only count in our back slot (they open, upgrade and dupe-check from it).
     * Edit the outfit with {@link #get}/{@link #set}; read it for looks and stats with this. Evaluated at most once
     * per tick per player; returns the same instance while nothing changes.
     */
    public static synchronized CosmeticInventory worn(LivingEntity entity) {
        CosmeticInventory own = get(entity);
        if (!(entity instanceof Player player) || !ExternalSlots.any()) {
            return own;
        }
        WornCache cached = WORN.get(player);
        if (cached != null && cached.tick() == player.tickCount && cached.own() == own) {
            return cached.merged();
        }
        Map<ResourceLocation, ItemStack> extra = new LinkedHashMap<>();
        ExternalSlots.forEach(player, stack -> {
            ResourceLocation slot = stack.isEmpty() ? null : slotOf(stack);
            if (slot != null && !FemboySlots.BACK.equals(slot) && own.get(slot).isEmpty() && !extra.containsKey(slot)) {
                extra.put(slot, stack);
            }
        });
        CosmeticInventory merged;
        if (cached != null && cached.own() == own && unchanged(cached, extra)) {
            merged = cached.merged();
        } else {
            merged = own;
            for (Map.Entry<ResourceLocation, ItemStack> entry : extra.entrySet()) {
                merged = merged.with(entry.getKey(), entry.getValue().copy());
            }
        }
        WORN.put(player, new WornCache(player.tickCount, own, Set.copyOf(extra.keySet()), merged));
        return merged;
    }

    /** Whether the other mods' slots still hold what the cached merge copied; by value, as their stacks may change in place. */
    private static boolean unchanged(WornCache cached, Map<ResourceLocation, ItemStack> extra) {
        if (!cached.extra().equals(extra.keySet())) {
            return false;
        }
        for (Map.Entry<ResourceLocation, ItemStack> entry : extra.entrySet()) {
            if (!ItemStack.matches(cached.merged().get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** The slot an item is worn in, or null if the item is not a cosmetic or its slot is unknown. */
    public static ResourceLocation slotOf(ItemStack stack) {
        Cosmetic cosmetic = FemboyComponents.COSMETIC.get(stack);
        if (cosmetic == null || slotRegistry.get(cosmetic.slot()).isEmpty()) {
            return null;
        }
        return cosmetic.slot();
    }

    /** Side-effect free check used by slots (both sides) and right-click equip. */
    public static boolean canEquip(LivingEntity entity, ResourceLocation slot, ItemStack stack) {
        if (stack.isEmpty() || !slot.equals(slotOf(stack)) || FemboyConfig.common().disabledSlots().contains(slot)) {
            return false;
        }
        return !FemboyMod.api().events().post(new CosmeticEquipEvent(entity, slot, stack)).isCancelled();
    }

    /**
     * Puts {@code stack} into the slot (ownership passes to the slot; pass a copy if you keep it).
     * On the server this posts events and syncs to the player and everyone tracking them.
     */
    public static void set(Player player, ResourceLocation slot, ItemStack stack) {
        CosmeticInventory before = PlatformHelper.getCosmetics(player);
        ItemStack previous = before.get(slot);
        PlatformHelper.setCosmetics(player, before.with(slot, stack));

        if (player instanceof ServerPlayer serverPlayer) {
            if (!ItemStack.matches(previous, stack)) {
                if (!previous.isEmpty()) {
                    FemboyMod.api().events().post(new CosmeticUnequipEvent(player, slot, previous));
                }
                FemboyMod.api().events().post(new CosmeticChangedEvent(player, slot, previous, stack));
                if (!stack.isEmpty()) {
                    // Audible feedback; heard by the wearer and players nearby, like equipping armor.
                    player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS,
                            EQUIP_SOUND_VOLUME, EQUIP_SOUND_PITCH);
                }
            }
            CosmeticsSyncPayload.sendToTrackingAndSelf(serverPlayer);
        }
    }

    /** Server: empties all slots and returns what was worn (used for death drops). */
    /** Show or hide a slot's item for everyone (it stays worn). */
    /** How one armor piece is drawn over the outfit; synced to everyone who sees the player. */
    public static void setArmorVisibility(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot armorSlot,
                                          dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility visibility) {
        PlatformHelper.setCosmetics(player, PlatformHelper.getCosmetics(player).withArmor(armorSlot, visibility));
        CosmeticsSyncPayload.sendToTrackingAndSelf(player);
    }

    public static void setHidden(ServerPlayer player, ResourceLocation slot, boolean hidden) {
        PlatformHelper.setCosmetics(player, PlatformHelper.getCosmetics(player).withHidden(slot, hidden));
        CosmeticsSyncPayload.sendToTrackingAndSelf(player);
    }

    public static List<ItemStack> clear(ServerPlayer player) {
        List<ItemStack> removed = List.copyOf(get(player).all().values());
        for (ResourceLocation slot : List.copyOf(get(player).all().keySet())) {
            set(player, slot, ItemStack.EMPTY);
        }
        return removed;
    }
}
