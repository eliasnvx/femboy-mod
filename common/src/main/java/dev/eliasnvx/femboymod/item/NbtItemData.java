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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

    public NbtItemData(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        Tag stored = tag.get(key);
        if (stored == null) {
            return null;
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
        return tag != null && tag.contains(key);
    }

    @Override
    public void set(ItemStack stack, T value) {
        codec.encodeStart(ops(), value).result().ifPresent(encoded -> stack.getOrCreateTag().put(key, encoded));
    }

    @Override
    public void remove(ItemStack stack) {
        stack.removeTagKey(key);
    }

    private static DynamicOps<Tag> ops() {
        RegistryAccess access = RegistryAccessContext.current();
        return access == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, access);
    }
}
