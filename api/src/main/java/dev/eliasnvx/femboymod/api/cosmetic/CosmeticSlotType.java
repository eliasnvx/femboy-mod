package dev.eliasnvx.femboymod.api.cosmetic;

import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * A cosmetic slot on a player (ears, socks, backpack, ...). Cosmetic slots are separate from
 * vanilla armor slots, so cosmetics are worn on top of any armor.
 *
 * <p>Register slot types in {@link dev.eliasnvx.femboymod.api.FemboyApi#cosmeticSlots()} during
 * {@link dev.eliasnvx.femboymod.api.FemboyAddon#onInitialize}. The slot's display name is the
 * translation key {@code cosmetic_slot.<namespace>.<path>} of its id.
 *
 * @param sortOrder     position in the slots screen; lower comes first. Built-in slots use
 *                      multiples of 100
 * @param emptySlotIcon GUI sprite shown while the slot is empty, if any
 */
public record CosmeticSlotType(int sortOrder, Optional<Identifier> emptySlotIcon) {

    /**
     * Creates a slot type without an empty-slot icon.
     *
     * @param sortOrder position in the slots screen
     */
    public CosmeticSlotType(int sortOrder) {
        this(sortOrder, Optional.empty());
    }

    /**
     * Returns the translation key for a slot id.
     *
     * @param slotId the slot id
     * @return {@code cosmetic_slot.<namespace>.<path>}
     */
    public static String translationKey(Identifier slotId) {
        return slotId.toLanguageKey("cosmetic_slot");
    }
}
