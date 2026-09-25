package dev.eliasnvx.femboymod.api.colorway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

/**
 * A stripe pattern for colorways, loaded from data packs at
 * {@code data/<namespace>/femboymod/colorway/<name>.json}.
 *
 * <p>Example ({@code trans.json}):
 * <pre>{@code
 * { "stripes": ["#5BCEFA", "#F5A9B8", "#FFFFFF", "#F5A9B8", "#5BCEFA"] }
 * }</pre>
 * A stripe may also be {@code "base"} or {@code "secondary"} to use the item's own colors, e.g.
 * {@code { "stripes": ["base", "secondary"] }} for two-tone stripes.
 *
 * <p>Optionally a {@code chevron} (like the Progress Pride flag) is drawn over the stripes from the left
 * edge, on surfaces that support 2D patterns (other surfaces show only the stripes):
 * <pre>{@code
 * "chevron": { "colors": ["#FFFFFF", "#F5A9B8", "#5BCEFA", "#784F17", "#000000"], "band_width": 0.07 }
 * }</pre>
 * {@code colors} go from the left edge toward the arrow tip; {@code band_width} is each band's width as a
 * fraction of the surface width.
 *
 * @param stripes stripes from top to bottom; never empty
 * @param chevron optional chevron drawn over the stripes
 */
public record ColorwayPattern(List<Stripe> stripes, Optional<Chevron> chevron) {

    /** Registry key of the {@code femboymod:colorway} data pack registry. */
    public static final ResourceKey<Registry<ColorwayPattern>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(FemboyApi.MOD_ID, "colorway"));

    /** Codec for the JSON file format. */
    public static final Codec<ColorwayPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Stripe.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("stripes").forGetter(ColorwayPattern::stripes),
            Chevron.CODEC.optionalFieldOf("chevron").forGetter(ColorwayPattern::chevron)
    ).apply(instance, ColorwayPattern::new));

    /**
     * Creates a pattern.
     *
     * @param stripes stripes from top to bottom
     * @throws IllegalArgumentException if {@code stripes} is empty
     */
    public ColorwayPattern {
        if (stripes.isEmpty()) {
            throw new IllegalArgumentException("A colorway pattern needs at least one stripe");
        }
        stripes = List.copyOf(stripes);
    }

    /**
     * Creates a stripes-only pattern.
     *
     * @param stripes stripes from top to bottom
     */
    public ColorwayPattern(List<Stripe> stripes) {
        this(stripes, Optional.empty());
    }

    /**
     * Returns the color at a point of a 2D surface, including the chevron. Does not allocate.
     *
     * @param u              0 (left edge) .. 1 (right edge)
     * @param v              0 (top) .. 1 (bottom)
     * @param baseColor      item base color
     * @param secondaryColor item secondary color
     * @return {@code 0xRRGGBB}
     */
    public int colorAt(float u, float v, int baseColor, int secondaryColor) {
        if (chevron.isPresent()) {
            int band = chevron.get().bandAt(u, v);
            if (band >= 0) {
                return chevron.get().colors().get(band);
            }
        }
        int index = Math.min(stripes.size() - 1, Math.max(0, (int) (v * stripes.size())));
        return stripes.get(index).resolve(baseColor, secondaryColor);
    }

    /**
     * A Progress-style chevron: nested arrow bands pointing right from the left edge.
     *
     * @param colors    band colors from the left edge toward the tip, {@code 0xRRGGBB}
     * @param bandWidth each band's width as a fraction of the surface width
     */
    public record Chevron(List<Integer> colors, float bandWidth) {

        /** JSON codec. */
        public static final Codec<Chevron> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Colors.RGB_HEX_CODEC.listOf(1, 16).fieldOf("colors").forGetter(Chevron::colors),
                Codec.floatRange(0.01F, 0.5F).fieldOf("band_width").forGetter(Chevron::bandWidth)
        ).apply(instance, Chevron::new));

        /** Copies the color list. */
        public Chevron {
            colors = List.copyOf(colors);
        }

        /**
         * @param u 0..1 from the left edge
         * @param v 0..1 from the top
         * @return band index, or -1 if the point is outside the chevron
         */
        public int bandAt(float u, float v) {
            float distance = u + Math.abs(v - 0.5F);
            int band = (int) (distance / bandWidth);
            return band < colors.size() ? band : -1;
        }
    }

    /**
     * Resolves the stripes to concrete RGB colors.
     *
     * @param baseColor      the item's base color ({@code 0xRRGGBB})
     * @param secondaryColor the item's secondary color ({@code 0xRRGGBB}); used for
     *                       {@code "secondary"} stripes
     * @return one RGB value per stripe, top to bottom; a new array on every call
     */
    public int[] resolve(int baseColor, int secondaryColor) {
        int[] result = new int[stripes.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = stripes.get(i).resolve(baseColor, secondaryColor);
        }
        return result;
    }

    /** One stripe of a {@link ColorwayPattern}. */
    public sealed interface Stripe {

        /** Serialized as {@code "#RRGGBB"}, {@code "base"} or {@code "secondary"}. */
        Codec<Stripe> CODEC = Codec.STRING.comapFlatMap(Stripe::parse, Stripe::serialize);

        /**
         * Resolves this stripe to an RGB color.
         *
         * @param baseColor      item base color
         * @param secondaryColor item secondary color
         * @return {@code 0xRRGGBB}
         */
        int resolve(int baseColor, int secondaryColor);

        private static com.mojang.serialization.DataResult<Stripe> parse(String value) {
            return switch (value) {
                case "base" -> com.mojang.serialization.DataResult.success(Base.INSTANCE);
                case "secondary" -> com.mojang.serialization.DataResult.success(Secondary.INSTANCE);
                default -> Colors.parseHex(value).map(Fixed::new);
            };
        }

        private static String serialize(Stripe stripe) {
            return switch (stripe) {
                case Base b -> "base";
                case Secondary s -> "secondary";
                case Fixed f -> Colors.toHex(f.rgb());
            };
        }

        /**
         * A fixed color stripe.
         *
         * @param rgb {@code 0xRRGGBB}
         */
        record Fixed(int rgb) implements Stripe {
            /** Drops alpha bits. */
            public Fixed {
                rgb &= 0xFFFFFF;
            }

            @Override
            public int resolve(int baseColor, int secondaryColor) {
                return rgb;
            }
        }

        /** A stripe using the item's base color. */
        enum Base implements Stripe {
            /** Singleton. */
            INSTANCE;

            @Override
            public int resolve(int baseColor, int secondaryColor) {
                return baseColor;
            }
        }

        /** A stripe using the item's secondary color. */
        enum Secondary implements Stripe {
            /** Singleton. */
            INSTANCE;

            @Override
            public int resolve(int baseColor, int secondaryColor) {
                return secondaryColor;
            }
        }
    }
}
