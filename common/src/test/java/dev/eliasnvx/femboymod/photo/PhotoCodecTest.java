package dev.eliasnvx.femboymod.photo;

import java.util.Random;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoCodecTest {

    private static int[] gradient() {
        int[] argb = new int[PhotoCodec.RAW_BYTES];
        for (int y = 0; y < PhotoCodec.SIZE; y++) {
            for (int x = 0; x < PhotoCodec.SIZE; x++) {
                argb[y * PhotoCodec.SIZE + x] = 0xFF000000 | (x * 2) << 16 | (y * 2) << 8 | 0x80;
            }
        }
        return argb;
    }

    @Test
    void roundTripStaysCloseAndFitsAPacket() {
        int[] original = gradient();
        byte[] data = PhotoCodec.encode(original);
        assertTrue(data.length < 32_000, "fits a serverbound packet: " + data.length);
        int[] back = PhotoCodec.decode(data).orElseThrow();
        long error = 0;
        for (int i = 0; i < original.length; i++) {
            error += Math.abs((original[i] >> 16 & 0xFF) - (back[i] >> 16 & 0xFF));
        }
        assertTrue(error / original.length < 30, "average red error " + error / original.length);
        assertArrayEquals(back, PhotoCodec.decode(PhotoCodec.encode(back)).orElseThrow(), "palette colors encode to themselves... nearly");
    }

    @Test
    void noisyPhotosStillFit() {
        Random random = new Random(1);
        int[] noise = new int[PhotoCodec.RAW_BYTES];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = 0xFF000000 | random.nextInt(0xFFFFFF);
        }
        assertTrue(PhotoCodec.encode(noise).length <= PhotoCodec.MAX_BYTES);
    }

    @Test
    void rejectsBadData() {
        assertTrue(PhotoCodec.decode(new byte[0]).isEmpty());
        assertTrue(PhotoCodec.decode(new byte[]{1, 2, 3, 4}).isEmpty(), "not deflate");
        assertTrue(PhotoCodec.decode(deflate(new byte[PhotoCodec.RAW_BYTES - 1])).isEmpty(), "too short");
        assertTrue(PhotoCodec.decode(deflate(new byte[PhotoCodec.RAW_BYTES * 64])).isEmpty(), "zip bomb / too long");
        byte[] badIndex = new byte[PhotoCodec.RAW_BYTES];
        badIndex[5] = (byte) 255;
        assertTrue(PhotoCodec.decode(deflate(badIndex)).isEmpty(), "palette index out of range");
        assertEquals(PhotoCodec.RAW_BYTES, PhotoCodec.decode(deflate(new byte[PhotoCodec.RAW_BYTES])).orElseThrow().length);
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        byte[] out = new byte[raw.length + 1024];
        int n = deflater.deflate(out);
        deflater.end();
        return java.util.Arrays.copyOf(out, n);
    }
}
