package dev.eliasnvx.femboymod.api.colorway;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;

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
 * @param stripes stripes from top to bottom; never empty
 */
public record ColorwayPattern(List<Stripe> stripes) {

    /** Registry key of the {@code femboymod:colorway} data pack registry. */
    public static final ResourceKey<Registry<ColorwayPattern>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(FemboyApi.MOD_ID, "colorway"));

    /** Codec for the JSON file format. */
    public static final Codec<ColorwayPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Stripe.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("stripes").forGetter(ColorwayPattern::stripes)
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
