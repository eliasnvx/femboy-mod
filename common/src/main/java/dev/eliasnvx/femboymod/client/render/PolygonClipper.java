package dev.eliasnvx.femboymod.client.render;

/**
 * Cuts a convex polygon down to the inside of a plane (Sutherland-Hodgman). A vertex is {@link #STRIDE} floats,
 * position first; every attribute is interpolated linearly where an edge is cut. Pure math, covered by JUnit.
 */
public final class PolygonClipper {

    /** Position (3), color (4), uv0 (2), uv1 (2), uv2 (2), uv3 (2), normal (3). */
    public static final int STRIDE = 18;

    private PolygonClipper() {
    }

    /**
     * Keeps the part of the polygon in {@code in} where {@code a*x + b*y + c*z + d >= 0}, in the same winding.
     *
     * @param count vertices in {@code in}
     * @param out   room for {@code count + 1} vertices
     * @return vertices written to {@code out}; fewer than 3 means nothing is left
     */
    public static int clip(float[] in, int count, float[] out, float a, float b, float c, float d) {
        int written = 0;
        int previous = count - 1;
        float previousSide = side(in, previous, a, b, c, d);
        for (int i = 0; i < count; i++) {
            float side = side(in, i, a, b, c, d);
            if (side >= 0 != previousSide >= 0) {
                lerp(in, previous, i, previousSide / (previousSide - side), out, written++);
            }
            if (side >= 0) {
                System.arraycopy(in, i * STRIDE, out, written++ * STRIDE, STRIDE);
            }
            previous = i;
            previousSide = side;
        }
        return written;
    }

    private static float side(float[] vertices, int vertex, float a, float b, float c, float d) {
        int o = vertex * STRIDE;
        return a * vertices[o] + b * vertices[o + 1] + c * vertices[o + 2] + d;
    }

    private static void lerp(float[] in, int from, int to, float t, float[] out, int vertex) {
        int f = from * STRIDE;
        int s = to * STRIDE;
        int o = vertex * STRIDE;
        for (int k = 0; k < STRIDE; k++) {
            out[o + k] = in[f + k] + t * (in[s + k] - in[f + k]);
        }
    }
}
