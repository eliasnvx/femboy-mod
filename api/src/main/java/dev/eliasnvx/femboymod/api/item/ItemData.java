package dev.eliasnvx.femboymod.api.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A typed value stored on an {@link ItemStack}. Minecraft 1.20.1 has no data components, so Femboy Mod keeps
 * its item data in the stack's NBT under the key {@link #key()}, encoded with the value's codec.
 *
 * @param <T> the value type
 */
public interface ItemData<T> {

    /**
     * @return the NBT key the value is stored under, e.g. {@code femboymod:colorway}
     */
    String key();

    /**
     * @param stack the stack to read
     * @return the value, or null if the stack has none (or it cannot be decoded)
     */
    @Nullable T get(ItemStack stack);

    /**
     * @param stack    the stack to read
     * @param fallback returned when the stack has no value
     * @return the value or {@code fallback}
     */
    default T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value == null ? fallback : value;
    }

    /**
     * @param stack the stack to check
     * @return whether the stack carries a value
     */
    boolean has(ItemStack stack);

    /**
     * Stores {@code value} on the stack, replacing an existing one.
     *
     * @param stack the stack to change
     * @param value the value to store
     */
    void set(ItemStack stack, T value);

    /**
     * Removes the value from the stack.
     *
     * @param stack the stack to change
     */
    void remove(ItemStack stack);
}
