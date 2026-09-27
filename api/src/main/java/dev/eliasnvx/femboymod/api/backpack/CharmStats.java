package dev.eliasnvx.femboymod.api.backpack;

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
 * What a charm does while it hangs on a worn backpack (SPEC §5.3), loaded from the data pack registry
 * {@code femboymod:charm}. The entry id equals the charm item's id:
 * {@code data/<ns>/femboymod/charm/<item>.json} → {@code {"effects": [ ... ]}}.
 * Items also need the item tag {@code #femboymod:charms} to fit into charm slots.
 *
 * @param effects effects applied while the charm is on the backpack worn in the back slot
 */
public record CharmStats(List<ConfiguredEffect> effects) {

    /** Registry key of the {@code femboymod:charm} data pack registry. */
    public static final ResourceKey<Registry<CharmStats>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(FemboyApi.MOD_ID, "charm"));

    /** JSON codec. */
    public static final Codec<CharmStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            ConfiguredEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CharmStats::effects)
    ).apply(i, CharmStats::new));

    /** Copies the list. */
    public CharmStats {
        effects = List.copyOf(effects);
    }

    /**
     * @param item a charm item
     * @return the registry key holding its stats
     */
    public static ResourceKey<CharmStats> keyOf(Item item) {
        return ResourceKey.create(REGISTRY_KEY, BuiltInRegistries.ITEM.getKey(item));
    }
}
