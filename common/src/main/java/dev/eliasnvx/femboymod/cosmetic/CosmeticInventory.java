package dev.eliasnvx.femboymod.cosmetic;

import com.mojang.serialization.Codec;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticsView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of a player's cosmetic slots, stored as a player attachment.
 * Only non-empty slots are kept. Slots of removed addons are preserved so nothing is lost
 * if the addon is reinstalled.
 */
public record CosmeticInventory(Map<Identifier, ItemStack> items) implements CosmeticsView {

    public static final CosmeticInventory EMPTY = new CosmeticInventory(Map.of());

    public static final Codec<CosmeticInventory> CODEC = Codec.unboundedMap(Identifier.CODEC, ItemStack.CODEC)
            .xmap(CosmeticInventory::new, CosmeticInventory::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticInventory> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Identifier, ItemStack, Map<Identifier, ItemStack>>map(
                            HashMap::new, Identifier.STREAM_CODEC, ItemStack.STREAM_CODEC)
                    .map(CosmeticInventory::new, CosmeticInventory::items);

    public CosmeticInventory {
        Map<Identifier, ItemStack> copy = new LinkedHashMap<>();
        items.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                copy.put(slot, stack);
            }
        });
        items = java.util.Collections.unmodifiableMap(copy);
    }

    public CosmeticInventory with(Identifier slot, ItemStack stack) {
        Map<Identifier, ItemStack> next = new LinkedHashMap<>(items);
        if (stack.isEmpty()) {
            next.remove(slot);
        } else {
            next.put(slot, stack);
        }
        return new CosmeticInventory(next);
    }

    @Override
    public ItemStack get(Identifier slot) {
        return items.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public Map<Identifier, ItemStack> all() {
        return items;
    }
}
