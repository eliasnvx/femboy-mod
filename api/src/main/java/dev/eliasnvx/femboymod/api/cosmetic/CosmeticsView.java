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
}
