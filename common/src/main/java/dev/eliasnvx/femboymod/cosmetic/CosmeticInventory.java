package dev.eliasnvx.femboymod.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticsView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of a player's cosmetic slots, stored as a player attachment.
 * Only non-empty slots are kept. Slots of removed addons are preserved so nothing is lost
 * if the addon is reinstalled.
 */
public record CosmeticInventory(Map<Identifier, ItemStack> items, Set<Identifier> hidden) implements CosmeticsView {

    public static final CosmeticInventory EMPTY = new CosmeticInventory(Map.of());

    private static final Codec<Map<Identifier, ItemStack>> ITEMS_CODEC = Codec.unboundedMap(Identifier.CODEC, ItemStack.CODEC);

    /** {"items": {...}, "hidden": [...]}; worlds saved before hiding existed store the plain slot map. */
    public static final Codec<CosmeticInventory> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(i -> i.group(
                    ITEMS_CODEC.fieldOf("items").forGetter(CosmeticInventory::items),
                    Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("hidden", Set.of())
                            .forGetter(CosmeticInventory::hidden)
            ).apply(i, CosmeticInventory::new)),
            ITEMS_CODEC, CosmeticInventory::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticInventory> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<RegistryFriendlyByteBuf, Identifier, ItemStack, Map<Identifier, ItemStack>>map(
                    HashMap::new, Identifier.STREAM_CODEC, ItemStack.STREAM_CODEC), CosmeticInventory::items,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), inventory -> new HashSet<>(inventory.hidden()),
            CosmeticInventory::new);

    public CosmeticInventory(Map<Identifier, ItemStack> items) {
        this(items, Set.of());
    }

    public CosmeticInventory {
        Map<Identifier, ItemStack> copy = new LinkedHashMap<>();
        items.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                copy.put(slot, stack);
            }
        });
        items = java.util.Collections.unmodifiableMap(copy);
        hidden = Set.copyOf(hidden);
    }

    /** Hidden slots keep their item and effects but are not drawn. The flag stays when the slot is emptied. */
    public CosmeticInventory withHidden(Identifier slot, boolean hide) {
        Set<Identifier> next = new HashSet<>(hidden);
        if (hide) {
            next.add(slot);
        } else {
            next.remove(slot);
        }
        return new CosmeticInventory(items, next);
    }

    @Override
    public boolean isHidden(Identifier slot) {
        return hidden.contains(slot);
    }

    public CosmeticInventory with(Identifier slot, ItemStack stack) {
        Map<Identifier, ItemStack> next = new LinkedHashMap<>(items);
        if (stack.isEmpty()) {
            next.remove(slot);
        } else {
            next.put(slot, stack);
        }
        return new CosmeticInventory(next, hidden);
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
