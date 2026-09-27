package dev.eliasnvx.femboymod.api.cosmetic;

import net.minecraft.world.entity.EquipmentSlot;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * A cosmetic slot on a player (ears, socks, backpack, ...). Cosmetic slots are separate from
 * vanilla armor slots, so cosmetics are worn together with any armor. Armor pieces listed in
 * {@code coversArmor} are hidden (not removed) while the slot holds a visible item, so the armor doesn't cover
 * the outfit; players can override this per armor piece ({@link ArmorVisibility}).
 *
 * <p>Register slot types in {@link dev.eliasnvx.femboymod.api.FemboyApi#cosmeticSlots()} during
 * {@link dev.eliasnvx.femboymod.api.FemboyAddon#onInitialize}. The slot's display name is the
 * translation key {@code cosmetic_slot.<namespace>.<path>} of its id.
 *
 * @param sortOrder     position in the slots screen; lower comes first. Built-in slots use
 *                      multiples of 100
 * @param emptySlotIcon GUI sprite shown while the slot is empty, if any
 * @param coversArmor   armor slots ({@code HEAD}, {@code CHEST}, {@code LEGS}, {@code FEET}) hidden by default while
 *                      this slot is worn
 */
public record CosmeticSlotType(int sortOrder, Optional<ResourceLocation> emptySlotIcon, Set<EquipmentSlot> coversArmor) {

    /** Copies the armor set. */
    public CosmeticSlotType {
        coversArmor = Set.copyOf(coversArmor);
    }

    /**
     * Creates a slot type that doesn't hide armor.
     *
     * @param sortOrder     position in the slots screen
     * @param emptySlotIcon GUI sprite shown while the slot is empty, if any
     */
    public CosmeticSlotType(int sortOrder, Optional<ResourceLocation> emptySlotIcon) {
        this(sortOrder, emptySlotIcon, Set.of());
    }

    /**
     * Creates a slot type without an empty-slot icon that doesn't hide armor.
     *
     * @param sortOrder position in the slots screen
     */
    public CosmeticSlotType(int sortOrder) {
        this(sortOrder, Optional.empty(), Set.of());
    }

    /**
     * Returns the translation key for a slot id.
     *
     * @param slotId the slot id
     * @return {@code cosmetic_slot.<namespace>.<path>}
     */
    public static String translationKey(ResourceLocation slotId) {
        return slotId.toLanguageKey("cosmetic_slot");
    }
}
