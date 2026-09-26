package dev.eliasnvx.femboymod.api.cosmetic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Read-only snapshot of the cosmetics an entity is wearing.
 *
 * <p>Returned stacks are the live stacks: <b>do not modify them</b>. Copy them if you need to.
 * Available on both logical sides; on the client it reflects the last state synced by the server.
 */
public interface CosmeticsView {

    /**
     * Returns the item worn in a slot.
     *
     * @param slot the slot id
     * @return the stack, or {@link ItemStack#EMPTY}
     */
    ItemStack get(Identifier slot);

    /**
     * Returns all non-empty slots.
     *
     * @return an unmodifiable map of slot id to stack
     */
    Map<Identifier, ItemStack> all();

    /**
     * Returns whether nothing is worn.
     *
     * @return {@code true} if all slots are empty
     */
    default boolean isEmpty() {
        return all().isEmpty();
    }

    /**
     * Whether the wearer hid this slot: the item stays worn (effects, Drip, set bonuses still count) but
     * renderers must not draw it. Built-in renderers already skip hidden slots.
     *
     * @param slot the slot id
     * @return {@code true} if hidden
     */
    default boolean isHidden(Identifier slot) {
        return false;
    }

    /**
     * Returns the wearer's choice for an armor piece. The effective look also depends on the slots worn and the
     * server's game rule.
     *
     * @param armorSlot {@code HEAD}, {@code CHEST}, {@code LEGS} or {@code FEET}
     * @return the chosen visibility; {@link ArmorVisibility#AUTO} unless the wearer changed it
     */
    @org.jetbrains.annotations.ApiStatus.AvailableSince("0.1.0")
    default ArmorVisibility armorVisibility(net.minecraft.world.entity.EquipmentSlot armorSlot) {
        return ArmorVisibility.AUTO;
    }
}
