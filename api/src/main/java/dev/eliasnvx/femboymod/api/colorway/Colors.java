package dev.eliasnvx.femboymod.api.colorway;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** Helpers for {@code 0xRRGGBB} colors written as {@code "#RRGGBB"} strings in JSON. */
public final class Colors {

    /** Codec for {@code "#RRGGBB"} strings. Also accepts a plain integer for convenience. */
    public static final Codec<Integer> RGB_HEX_CODEC = Codec.either(
                    Codec.STRING.comapFlatMap(Colors::parseHex, Colors::toHex),
                    Codec.intRange(0, 0xFFFFFF))
            // 1.20.1 DFU has no withAlternative: read either form, always write the hex string
            .xmap(either -> either.map(rgb -> rgb, rgb -> rgb), Either::left);

    private Colors() {
    }

    /**
     * Parses {@code "#RRGGBB"} (case-insensitive).
     *
     * @param value the string
     * @return the RGB value, or an error result
     */
    public static DataResult<Integer> parseHex(String value) {
        if (value.length() != 7 || value.charAt(0) != '#') {
            return DataResult.error(() -> "Expected a color like \"#RRGGBB\", got \"" + value + "\"");
        }
        try {
            return DataResult.success(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid hex color \"" + value + "\"");
        }
    }

    /**
     * Formats an RGB value as {@code "#RRGGBB"} (upper case).
     *
     * @param rgb {@code 0xRRGGBB}; alpha bits are ignored
     * @return the hex string
     */
    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
