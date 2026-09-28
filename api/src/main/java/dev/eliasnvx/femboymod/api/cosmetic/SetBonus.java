package dev.eliasnvx.femboymod.api.cosmetic;

import dev.eliasnvx.femboymod.api.util.ListCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.effect.ConfiguredEffect;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * A cosmetic set, loaded from {@code data/<ns>/femboymod/set_bonus/<name>.json} (SPEC §4.2).
 * <pre>{@code
 * { "pieces": ["femboymod:cat_ears", "#femboymod:socks"],
 *   "required": 3,
 *   "scaling_per_tier": 0.1,
 *   "effects": [ ... ] }
 * }</pre>
 * Each piece is an item, a list of items or an item tag; a piece counts when any worn cosmetic matches it.
 * Display name: lang key {@code set_bonus.<ns>.<name>}.
 *
 * @param pieces         the pieces
 * @param required       how many pieces must be worn; all of them if empty
 * @param scalingPerTier extra strength per Drip tier: effects get {@code scale = 1 + tier * scalingPerTier}
 * @param effects        effects applied while the set is complete
 */
public record SetBonus(List<HolderSet<Item>> pieces, Optional<Integer> required, double scalingPerTier,
                       List<ConfiguredEffect> effects) {

    /** Registry key of the {@code femboymod:set_bonus} data pack registry. */
    public static final ResourceKey<Registry<SetBonus>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(FemboyApi.MOD_ID, "set_bonus"));

    /** JSON codec. */
    public static final Codec<SetBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ListCodecs.sized(RegistryCodecs.homogeneousList(Registries.ITEM), 1, Integer.MAX_VALUE).fieldOf("pieces").forGetter(SetBonus::pieces),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("required").forGetter(SetBonus::required),
            Codec.doubleRange(0, 10).optionalFieldOf("scaling_per_tier", 0.0).forGetter(SetBonus::scalingPerTier),
            ConfiguredEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SetBonus::effects)
    ).apply(instance, SetBonus::new));

    /** Copies lists. */
    public SetBonus {
        pieces = List.copyOf(pieces);
        effects = List.copyOf(effects);
    }

    /**
     * @return number of pieces that must be worn
     */
    public int requiredCount() {
        return Math.min(required.orElse(pieces.size()), pieces.size());
    }
}
