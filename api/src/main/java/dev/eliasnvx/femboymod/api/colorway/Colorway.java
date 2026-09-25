package dev.eliasnvx.femboymod.api.colorway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Data component {@code femboymod:colorway}: the colors of a dyeable cosmetic.
 *
 * <p>JSON form: {@code {"base_color": "#FFB6D9", "pattern": "femboymod:pride_trans",
 * "secondary_color": "#FFFFFF"}}; {@code pattern} and {@code secondary_color} are optional.
 * Without a pattern the item is a solid {@code base_color}.
 *
 * @param baseColor      main color, {@code 0xRRGGBB}
 * @param pattern        stripe pattern from the {@code femboymod:colorway} data pack registry
 * @param secondaryColor color for {@code "secondary"} stripes; defaults to white
 */
public record Colorway(int baseColor, Optional<Holder<ColorwayPattern>> pattern, Optional<Integer> secondaryColor) {

    /** Secondary color used when none is set: white. */
    public static final int DEFAULT_SECONDARY = 0xFFFFFF;

    private static final int RGB_MASK = 0xFFFFFF;

    /** Persistent codec. */
    public static final Codec<Colorway> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Colors.RGB_HEX_CODEC.fieldOf("base_color").forGetter(Colorway::baseColor),
            RegistryFixedCodec.create(ColorwayPattern.REGISTRY_KEY).optionalFieldOf("pattern").forGetter(Colorway::pattern),
            Colors.RGB_HEX_CODEC.optionalFieldOf("secondary_color").forGetter(Colorway::secondaryColor)
    ).apply(instance, Colorway::new));

    /** Network codec; requires the colorway registry to be synced (it is). */
    public static final StreamCodec<RegistryFriendlyByteBuf, Colorway> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.RGB_COLOR, Colorway::baseColor,
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(ColorwayPattern.REGISTRY_KEY)), Colorway::pattern,
            ByteBufCodecs.optional(ByteBufCodecs.RGB_COLOR), Colorway::secondaryColor,
            Colorway::new);

    /**
     * Normalizes colors to 24-bit RGB: alpha bits are dropped, so values compare equal no matter
     * which codec produced them (vanilla's RGB stream codec adds an opaque alpha).
     */
    public Colorway {
        baseColor &= RGB_MASK;
        secondaryColor = secondaryColor.map(color -> color & RGB_MASK);
    }

    /**
     * Creates a solid colorway.
     *
     * @param baseColor {@code 0xRRGGBB}
     * @return the colorway
     */
    public static Colorway solid(int baseColor) {
        return new Colorway(baseColor, Optional.empty(), Optional.empty());
    }

    /**
     * Returns one stripe's color without allocating (safe to call every frame from renderers).
     *
     * @param index stripe index; wraps around the stripe count
     * @return {@code 0xRRGGBB}
     */
    public int stripeColor(int index) {
        if (pattern.isEmpty()) {
            return baseColor;
        }
        java.util.List<ColorwayPattern.Stripe> stripes = pattern.get().value().stripes();
        return stripes.get(Math.floorMod(index, stripes.size())).resolve(baseColor, secondaryColor.orElse(DEFAULT_SECONDARY));
    }

    /**
     * Returns the number of stripes (1 for a solid colorway).
     *
     * @return stripe count
     */
    public int stripeCount() {
        return pattern.map(p -> p.value().stripes().size()).orElse(1);
    }

    /**
     * Resolves the stripes to RGB colors, top to bottom. A solid colorway has one stripe.
     *
     * @return a new array of {@code 0xRRGGBB} values
     */
    public int[] resolveStripes() {
        return pattern.map(p -> p.value().resolve(baseColor, secondaryColor.orElse(DEFAULT_SECONDARY)))
                .orElseGet(() -> new int[]{baseColor});
    }
}
