package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Buffers that draw only what is seen through a window ({@link ClippingVertexConsumer}), for mirror reflections, so
 * they never spill past the glass. Quad geometry is clipped; leashes are dropped (a reflection has none, like in 26.3);
 * anything else passes through. Reused (render thread): {@link #begin} before drawing, {@link #end} after.
 */
final class ClippingBufferSource implements MultiBufferSource {

    /** Takes vertices and draws nothing. */
    private static final VertexConsumer DISCARD = new VertexConsumer() {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    };

    /** One per buffer handed out while drawing (item glint draws to two at once). */
    private final List<ClippingVertexConsumer> consumers = new ArrayList<>();
    private int used;
    private MultiBufferSource buffers;
    private float[] window;

    /** @param window four planes, see {@link ClippingVertexConsumer#begin} */
    MultiBufferSource begin(MultiBufferSource buffers, float[] window) {
        this.buffers = buffers;
        this.window = window;
        this.used = 0;
        return this;
    }

    void end() {
        for (int i = 0; i < used; i++) {
            consumers.get(i).end();
        }
        used = 0;
        buffers = null;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        // the source may close earlier buffers now: send their finished quads first
        for (int i = 0; i < used; i++) {
            consumers.get(i).flushIfComplete();
        }
        if (type == RenderType.leash()) {
            return DISCARD; // pets' leashes would run from the reflection to the real holder
        }
        VertexConsumer buffer = buffers.getBuffer(type);
        if (type.mode() != VertexFormat.Mode.QUADS) {
            return buffer;
        }
        if (used == consumers.size()) {
            consumers.add(new ClippingVertexConsumer());
        }
        return consumers.get(used++).begin(buffer, window);
    }
}
