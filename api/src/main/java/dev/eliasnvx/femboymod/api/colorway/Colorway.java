package dev.eliasnvx.femboymod.api.colorway;

import org.jetbrains.annotations.ApiStatus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;

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

    /**
     * Writes this colorway to a packet. The pattern is sent by id; the colorway registry is synced to clients.
     *
     * @param buf the packet buffer
     */
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(baseColor);
        buf.writeOptional(pattern.flatMap(Holder::unwrapKey).map(ResourceKey::location), FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(secondaryColor, FriendlyByteBuf::writeInt);
    }

    /**
     * Reads a colorway written by {@link #write}. A pattern unknown to {@code registries} is dropped.
     *
     * @param buf        the packet buffer
     * @param registries registries used to look the pattern up
     * @return the colorway
     */
    public static Colorway read(FriendlyByteBuf buf, RegistryAccess registries) {
        int base = buf.readInt();
        Optional<Holder<ColorwayPattern>> pattern = buf.readOptional(FriendlyByteBuf::readResourceLocation)
                .flatMap(id -> registries.registryOrThrow(ColorwayPattern.REGISTRY_KEY)
                        .getHolder(ResourceKey.create(ColorwayPattern.REGISTRY_KEY, id)))
                .map(holder -> holder);
        Optional<Integer> secondary = buf.readOptional(FriendlyByteBuf::readInt);
        return new Colorway(base, pattern, secondary);
    }

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
     * Returns the color at a point of a 2D surface (stripes plus a chevron if the pattern has one).
     * Does not allocate.
     *
     * @param u 0 (left) .. 1 (right)
     * @param v 0 (top) .. 1 (bottom)
     * @return {@code 0xRRGGBB}
     */
    public int colorAt(float u, float v) {
        return colorAt(u, v, 0.0F);
    }

    /**
     * Returns one stripe's color at a point in time: patterns with a {@code shimmer} flow smoothly between
     * their stripe colors. Does not allocate.
     *
     * @param index stripe index; wraps around the stripe count
     * @param ticks animation time in ticks (0 gives the resting colors)
     * @return {@code 0xRRGGBB}
     */
    @ApiStatus.AvailableSince("0.1.0")
    public int stripeColor(int index, float ticks) {
        if (pattern.isEmpty()) {
            return baseColor;
        }
        return pattern.get().value().stripeColor(index, ticks, baseColor, secondaryColor.orElse(DEFAULT_SECONDARY));
    }

    /**
     * {@link #colorAt(float, float)} at a point in time (see {@link #stripeColor(int, float)}).
     *
     * @param u     0 (left) .. 1 (right)
     * @param v     0 (top) .. 1 (bottom)
     * @param ticks animation time in ticks
     * @return {@code 0xRRGGBB}
     */
    @ApiStatus.AvailableSince("0.1.0")
    public int colorAt(float u, float v, float ticks) {
        if (pattern.isEmpty()) {
            return baseColor;
        }
        return pattern.get().value().colorAt(u, v, ticks, baseColor, secondaryColor.orElse(DEFAULT_SECONDARY));
    }

    /**
     * @return whether the colors change over time (the pattern has a {@code shimmer})
     */
    @ApiStatus.AvailableSince("0.1.0")
    public boolean hasShimmer() {
        return pattern.isPresent() && pattern.get().value().shimmer().isPresent();
    }

    /**
     * @return whether the pattern has a chevron (needs 2D surfaces to show)
     */
    public boolean hasChevron() {
        return pattern.isPresent() && pattern.get().value().chevron().isPresent();
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
