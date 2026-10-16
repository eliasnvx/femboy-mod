package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Passes quads on cut down to a window: the inside of four planes through the eye and the window's edges, so only
 * what is seen through the window gets drawn ({@link PolygonClipper}). A quad goes out when its fourth vertex ends.
 * For quad geometry only. {@link ClippingBufferSource} hands them out (render thread).
 */
final class ClippingVertexConsumer implements VertexConsumer {

    private static final int STRIDE = PolygonClipper.STRIDE;
    /** A quad cut by four planes has at most eight corners. */
    private static final int MAX_VERTICES = 8;
    private static final int PLANES = 4;
    private static final int COLOR = 1;
    private static final int UV0 = 2;
    private static final int UV1 = 4;
    private static final int UV2 = 8;
    private static final int NORMAL = 32;

    private final float[] first = new float[MAX_VERTICES * STRIDE];
    private final float[] second = new float[MAX_VERTICES * STRIDE];
    private VertexConsumer out;
    private float[] planes;
    private int count;
    private int attributes;

    /**
     * @param planes four planes as {@code a, b, c, d}: inside where {@code a*x + b*y + c*z + d >= 0}, in the space of
     *               the positions this consumer gets
     */
    VertexConsumer begin(VertexConsumer out, float[] planes) {
        this.out = out;
        this.planes = planes;
        this.count = 0;
        this.attributes = 0;
        return this;
    }

    void end() {
        count = 0;
        attributes = 0;
        out = null;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        if (count == 4) {
            flush();
        }
        int o = count++ * STRIDE;
        first[o] = (float) x;
        first[o + 1] = (float) y;
        first[o + 2] = (float) z;
        return this;
    }

    private int current() {
        return (count - 1) * STRIDE;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        if (count > 0) {
            int o = current();
            first[o + 3] = red;
            first[o + 4] = green;
            first[o + 5] = blue;
            first[o + 6] = alpha;
            attributes |= COLOR;
        }
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        return pair(7, u, v, UV0);
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return pair(9, u, v, UV1);
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return pair(11, u, v, UV2);
    }

    private VertexConsumer pair(int offset, float u, float v, int attribute) {
        if (count > 0) {
            int o = current() + offset;
            first[o] = u;
            first[o + 1] = v;
            attributes |= attribute;
        }
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        if (count > 0) {
            int o = current() + 15;
            first[o] = x;
            first[o + 1] = y;
            first[o + 2] = z;
            attributes |= NORMAL;
        }
        return this;
    }

    @Override
    public void endVertex() {
        if (count == 4) {
            flush();
        }
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        out.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        out.unsetDefaultColor();
    }

    private void flush() {
        int n = count;
        count = 0;
        float[] source = first;
        float[] target = second;
        for (int p = 0; p < PLANES && n >= 3; p++) {
            int o = p * 4;
            n = PolygonClipper.clip(source, n, target, planes[o], planes[o + 1], planes[o + 2], planes[o + 3]);
            float[] swap = source;
            source = target;
            target = swap;
        }
        // a fan of quads over the convex result; an odd corner closes with a degenerate quad
        for (int i = 1; i + 1 < n; i += 2) {
            emit(source, 0);
            emit(source, i);
            emit(source, i + 1);
            emit(source, Math.min(i + 2, n - 1));
        }
        attributes = 0;
    }

    /** In the entity vertex format's order: position, color, uv, overlay, light, normal. */
    private void emit(float[] vertices, int vertex) {
        int o = vertex * STRIDE;
        out.vertex(vertices[o], vertices[o + 1], vertices[o + 2]);
        if ((attributes & COLOR) != 0) {
            out.color(Math.round(vertices[o + 3]), Math.round(vertices[o + 4]), Math.round(vertices[o + 5]), Math.round(vertices[o + 6]));
        }
        if ((attributes & UV0) != 0) {
            out.uv(vertices[o + 7], vertices[o + 8]);
        }
        if ((attributes & UV1) != 0) {
            out.overlayCoords(Math.round(vertices[o + 9]), Math.round(vertices[o + 10]));
        }
        if ((attributes & UV2) != 0) {
            out.uv2(Math.round(vertices[o + 11]), Math.round(vertices[o + 12]));
        }
        if ((attributes & NORMAL) != 0) {
            out.normal(vertices[o + 15], vertices[o + 16], vertices[o + 17]);
        }
        out.endVertex();
    }
}
