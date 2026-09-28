package dev.eliasnvx.femboymod.api.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.effect.ConfiguredEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Balance data of a cosmetic item, loaded from the data pack registry {@code femboymod:cosmetic_stats}.
 * The entry id must equal the item id: stats for {@code mymod:scarf} live in
 * {@code data/mymod/femboymod/cosmetic_stats/scarf.json}.
 * <pre>{@code
 * { "drip": 12,
 *   "effects": [ { "effect": { "type": "femboymod:attribute", "attribute": "minecraft:movement_speed",
 *                              "amount": 0.05, "operation": "add_multiplied_base" } } ] }
 * }</pre>
 * Items without an entry are pure cosmetics with 0 drip.
 *
 * @param drip    contribution to the Drip Level
 * @param effects effects applied while the item is worn
 */
public record CosmeticStats(int drip, List<ConfiguredEffect> effects) {

    /** Registry key of the {@code femboymod:cosmetic_stats} data pack registry. */
    public static final ResourceKey<Registry<CosmeticStats>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(FemboyApi.MOD_ID, "cosmetic_stats"));

    /** Stats of items without an entry. */
    public static final CosmeticStats NONE = new CosmeticStats(0, List.of());

    /** JSON codec. */
    public static final Codec<CosmeticStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 1000).optionalFieldOf("drip", 0).forGetter(CosmeticStats::drip),
            ConfiguredEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CosmeticStats::effects)
    ).apply(instance, CosmeticStats::new));

    /** Copies the effect list. */
    public CosmeticStats {
        effects = List.copyOf(effects);
    }

    /**
     * Returns the registry key holding the stats of an item.
     *
     * @param item the item
     * @return {@code femboymod:cosmetic_stats} key with the item's id
     */
    public static ResourceKey<CosmeticStats> keyOf(Item item) {
        return ResourceKey.create(REGISTRY_KEY, BuiltInRegistries.ITEM.getKey(item));
    }
}
