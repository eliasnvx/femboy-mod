package dev.eliasnvx.femboymod.api.effect;

import net.minecraft.resources.ResourceLocation;

/**
 * Identifies one active application of a {@link CosmeticEffect}.
 *
 * @param id    unique and stable while active, e.g. {@code femboymod:item/head_accessory/0} or
 *              {@code femboymod:set/full_femboy_mode/1}; use it as the id of attribute modifiers
 * @param scale strength multiplier (1.0 = as configured). Set bonuses with {@code scaling} grow
 *              with the wearer's Drip tier
 */
public record EffectSource(ResourceLocation id, double scale) {
}
