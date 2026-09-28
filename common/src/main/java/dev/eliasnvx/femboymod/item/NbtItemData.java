package dev.eliasnvx.femboymod.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import dev.eliasnvx.femboymod.api.item.ItemData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ItemData} stored in an item stack's NBT under {@code key}, encoded with {@code codec}. Decoded values are
 * cached by the identity of the stored tag, so reading the same stack every frame does not decode again.
 */
public final class NbtItemData<T> implements ItemData<T> {

    private static final int CACHE_SIZE = 4096;

    private final String key;
    private final Codec<T> codec;
    // Weak identity keys: an entry goes away with the tag; a changed value is a new tag instance
    private final Cache<Tag, Optional<T>> decoded = CacheBuilder.newBuilder().weakKeys().maximumSize(CACHE_SIZE).build();
    /** Per-item values for stacks without their own (1.21.1: the item's default components). */
    private final Map<Item, T> defaults = new ConcurrentHashMap<>();

    public NbtItemData(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
    }

    @Override
    public String key() {
        return key;
    }

    /**
     * Gives every stack of {@code item} this value unless the stack stores its own, like a default component on
     * 1.21.1. Call it when the item is created.
     */
    @Override
    public void setDefault(ItemLike item, T value) {
        defaults.put(item.asItem(), value);
    }

    /** @return the value every stack of {@code item} has by default, or null */
    public @Nullable T getDefault(ItemLike item) {
        return defaults.get(item.asItem());
    }

    @Override
    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        Tag stored = tag == null ? null : tag.get(key);
        if (stored == null) {
            return stack.isEmpty() ? null : defaults.get(stack.getItem());
        }
        Optional<T> cached = decoded.getIfPresent(stored);
        if (cached == null) {
            cached = codec.parse(ops(), stored).result();
            decoded.put(stored, cached);
        }
        return cached.orElse(null);
    }

    @Override
    public boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key) || !stack.isEmpty() && defaults.containsKey(stack.getItem());
    }

    @Override
    public void set(ItemStack stack, T value) {
        if (value.equals(defaults.get(stack.getItem()))) {
            remove(stack); // keep NBT minimal: stacks at their default stay stackable with fresh ones
            return;
        }
        codec.encodeStart(ops(), value).result().ifPresent(encoded -> stack.getOrCreateTag().put(key, encoded));
    }

    /** Removes the stack's own value; a stack of an item with a default falls back to it. */
    @Override
    public void remove(ItemStack stack) {
        stack.removeTagKey(key);
    }

    private static DynamicOps<Tag> ops() {
        RegistryAccess access = RegistryAccessContext.current();
        return access == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, access);
    }
}
