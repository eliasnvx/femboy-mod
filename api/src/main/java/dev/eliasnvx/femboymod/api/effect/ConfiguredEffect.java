package dev.eliasnvx.femboymod.api.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * An effect plus an optional condition, as written in cosmetic stats and set bonus JSON:
 * <pre>{@code
 * { "effect": { "type": "femboymod:mob_effect", "effect": "minecraft:regeneration" },
 *   "when":   { "type": "femboymod:cold_biome" } }
 * }</pre>
 *
 * @param effect the effect
 * @param when   active only while this holds; always active if empty
 */
public record ConfiguredEffect(CosmeticEffect effect, Optional<CosmeticCondition> when) {

    /** JSON codec. */
    public static final Codec<ConfiguredEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CosmeticEffect.CODEC.fieldOf("effect").forGetter(ConfiguredEffect::effect),
            CosmeticCondition.CODEC.optionalFieldOf("when").forGetter(ConfiguredEffect::when)
    ).apply(instance, ConfiguredEffect::new));
}
