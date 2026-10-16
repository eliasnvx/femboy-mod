package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;

/**
 * Passes quads on cut down to a window: the inside of four planes through the eye and the window's edges, so only
 * what is seen through the window gets drawn ({@link PolygonClipper}). Collects one quad at a time; {@link #end()}
 * sends the last one. For quad geometry only. One instance serves every window in turn (render thread).
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
    private static final int UV3 = 16;
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
        flush();
        out = null;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (count == 4) {
            flush();
        }
        int o = count++ * STRIDE;
        first[o] = x;
        first[o + 1] = y;
        first[o + 2] = z;
        return this;
    }

    private int current() {
        return (count - 1) * STRIDE;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
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
    public VertexConsumer setColor(int argb) {
        return setColor(ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb), ARGB.alpha(argb));
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return pair(7, u, v, UV0);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return pair(9, u, v, UV1);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return pair(11, u, v, UV2);
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return pair(13, u, v, UV3);
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
    public VertexConsumer setNormal(float x, float y, float z) {
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
    public VertexConsumer setLineWidth(float width) {
        return this; // quads only
    }

    private void flush() {
        int n = count;
        count = 0;
        if (n < 4) {
            attributes = 0;
            return;
        }
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

    private void emit(float[] vertices, int vertex) {
        int o = vertex * STRIDE;
        out.addVertex(vertices[o], vertices[o + 1], vertices[o + 2]);
        if ((attributes & COLOR) != 0) {
            out.setColor(Math.round(vertices[o + 3]), Math.round(vertices[o + 4]), Math.round(vertices[o + 5]), Math.round(vertices[o + 6]));
        }
        if ((attributes & UV0) != 0) {
            out.setUv(vertices[o + 7], vertices[o + 8]);
        }
        if ((attributes & UV1) != 0) {
            out.setUv1(Math.round(vertices[o + 9]), Math.round(vertices[o + 10]));
        }
        if ((attributes & UV2) != 0) {
            out.setUv2(Math.round(vertices[o + 11]), Math.round(vertices[o + 12]));
        }
        if ((attributes & UV3) != 0) {
            out.setUv3(vertices[o + 13], vertices[o + 14]);
        }
        if ((attributes & NORMAL) != 0) {
            out.setNormal(vertices[o + 15], vertices[o + 16], vertices[o + 17]);
        }
    }
}
