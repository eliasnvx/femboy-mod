package dev.eliasnvx.femboymod.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PolygonClipperTest {

    private static final int S = PolygonClipper.STRIDE;
    private static final float EPS = 1.0E-5F;

    /** A unit square in the xy plane; the u texture coordinate runs with x. */
    private static float[] square() {
        float[] v = new float[5 * S];
        float[][] corners = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            v[i * S] = corners[i][0];
            v[i * S + 1] = corners[i][1];
            v[i * S + 7] = corners[i][0];
        }
        return v;
    }

    @Test
    void insideStaysAsItIs() {
        float[] in = square();
        float[] out = new float[5 * S];
        int n = PolygonClipper.clip(in, 4, out, 1, 0, 0, 1); // x >= -1
        assertEquals(4, n);
        assertArrayEquals(java.util.Arrays.copyOf(in, 4 * S), java.util.Arrays.copyOf(out, 4 * S), EPS);
    }

    @Test
    void outsideIsGone() {
        float[] out = new float[5 * S];
        assertEquals(0, PolygonClipper.clip(square(), 4, out, 1, 0, 0, -2)); // x >= 2
    }

    @Test
    void halfIsCutAndInterpolated() {
        float[] out = new float[5 * S];
        int n = PolygonClipper.clip(square(), 4, out, -1, 0, 0, 0.5F); // x <= 0.5
        assertEquals(4, n);
        for (int i = 0; i < n; i++) {
            float x = out[i * S];
            assertEquals(true, x <= 0.5F + EPS, "x within the plane");
            assertEquals(x, out[i * S + 7], EPS, "u follows x");
        }
    }

    @Test
    void cornerCutAddsAVertex() {
        float[] out = new float[5 * S];
        int n = PolygonClipper.clip(square(), 4, out, -1, -1, 0, 1.5F); // x + y <= 1.5
        assertEquals(5, n);
    }
}
