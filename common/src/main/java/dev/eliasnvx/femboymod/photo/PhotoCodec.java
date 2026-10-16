package dev.eliasnvx.femboymod.photo;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Photo pixels on the wire and on disk: {@link #SIZE}x{@link #SIZE}, one byte per pixel into a fixed palette of
 * 6x7x6 colors (like a map, a bit richer), ordered dithering, then deflate. At most {@link #RAW_BYTES} after
 * inflating, so a photo always fits in one packet. Pure Java, covered by JUnit.
 */
public final class PhotoCodec {

    public static final int SIZE = 128;
    public static final int RAW_BYTES = SIZE * SIZE;
    /** Compressed size limit; the raw data can never deflate to more than a little above {@link #RAW_BYTES}. */
    public static final int MAX_BYTES = RAW_BYTES + 1024;

    private static final int RED_LEVELS = 6;
    private static final int GREEN_LEVELS = 7;
    private static final int BLUE_LEVELS = 6;
    public static final int PALETTE_SIZE = RED_LEVELS * GREEN_LEVELS * BLUE_LEVELS;
    /** 4x4 Bayer matrix, values 0..15. */
    /** Below 1 the dither pattern is softer (a little more banding in smooth skies). */
    private static final float DITHER_STRENGTH = 0.7F;
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private PhotoCodec() {
    }

    /** Opaque ARGB color of a palette index. */
    public static int color(int index) {
        int r = index / (GREEN_LEVELS * BLUE_LEVELS);
        int g = index / BLUE_LEVELS % GREEN_LEVELS;
        int b = index % BLUE_LEVELS;
        return 0xFF000000 | level(r, RED_LEVELS) << 16 | level(g, GREEN_LEVELS) << 8 | level(b, BLUE_LEVELS);
    }

    private static int level(int step, int levels) {
        return step * 255 / (levels - 1);
    }

    /** Nearest palette step for a channel value, nudged by the dither threshold (-0.5..0.5 of a step). */
    private static int step(int value, int levels, float dither) {
        float scaled = value * (levels - 1) / 255.0F + dither;
        return Math.max(0, Math.min(levels - 1, Math.round(scaled)));
    }

    /**
     * @param argb {@link #SIZE}x{@link #SIZE} pixels, row by row
     * @return deflated palette indices
     */
    public static byte[] encode(int[] argb) {
        if (argb.length != RAW_BYTES) {
            throw new IllegalArgumentException("photo must be " + SIZE + "x" + SIZE);
        }
        byte[] raw = new byte[RAW_BYTES];
        for (int i = 0; i < RAW_BYTES; i++) {
            float dither = ((BAYER[(i / SIZE % 4) * 4 + i % SIZE % 4] + 0.5F) / 16.0F - 0.5F) * DITHER_STRENGTH;
            int c = argb[i];
            int r = step(c >> 16 & 0xFF, RED_LEVELS, dither);
            int g = step(c >> 8 & 0xFF, GREEN_LEVELS, dither);
            int b = step(c & 0xFF, BLUE_LEVELS, dither);
            raw[i] = (byte) ((r * GREEN_LEVELS + g) * BLUE_LEVELS + b);
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(RAW_BYTES / 2);
        byte[] buffer = new byte[4096];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    /**
     * @return the ARGB pixels, or empty if the data is not a valid photo (wrong size, too big when inflated,
     * palette index out of range, broken deflate stream)
     */
    public static Optional<int[]> decode(byte[] data) {
        if (data.length == 0 || data.length > MAX_BYTES) {
            return Optional.empty();
        }
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            byte[] raw = new byte[RAW_BYTES];
            int read = 0;
            while (read < RAW_BYTES && !inflater.finished()) {
                int n = inflater.inflate(raw, read, RAW_BYTES - read);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    return Optional.empty();
                }
                read += n;
            }
            // exactly RAW_BYTES, and nothing more hidden after them
            if (read != RAW_BYTES || !inflater.finished() && inflater.inflate(new byte[1]) > 0) {
                return Optional.empty();
            }
            int[] argb = new int[RAW_BYTES];
            for (int i = 0; i < RAW_BYTES; i++) {
                int index = raw[i] & 0xFF;
                if (index >= PALETTE_SIZE) {
                    return Optional.empty();
                }
                argb[i] = color(index);
            }
            return Optional.of(argb);
        } catch (DataFormatException e) {
            return Optional.empty();
        } finally {
            inflater.end();
        }
    }
}
