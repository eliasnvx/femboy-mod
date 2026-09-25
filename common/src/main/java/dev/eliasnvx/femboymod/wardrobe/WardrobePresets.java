package dev.eliasnvx.femboymod.wardrobe;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A player's saved outfits (SPEC §6.1): up to {@link #COUNT} presets, each a map slot → item (count 1).
 * Stored as a player attachment (not in the wardrobe block), so presets work with any wardrobe.
 */
public record WardrobePresets(Map<String, Map<Identifier, ItemStack>> presets) {

    public static final int COUNT = 5;
    public static final WardrobePresets EMPTY = new WardrobePresets(Map.of());

    public static final Codec<WardrobePresets> CODEC = Codec.unboundedMap(Codec.STRING,
                    Codec.unboundedMap(Identifier.CODEC, ItemStack.CODEC))
            .xmap(WardrobePresets::new, WardrobePresets::presets);

    public WardrobePresets {
        presets = Map.copyOf(presets);
    }

    public Optional<Map<Identifier, ItemStack>> get(int index) {
        return Optional.ofNullable(presets.get(Integer.toString(index)));
    }

    public WardrobePresets with(int index, Map<Identifier, ItemStack> outfit) {
        Map<String, Map<Identifier, ItemStack>> next = new HashMap<>(presets);
        next.put(Integer.toString(index), Map.copyOf(outfit));
        return new WardrobePresets(next);
    }
}
