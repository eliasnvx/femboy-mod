package dev.eliasnvx.femboymod.client;

/**
 * Pixel math for the Photo Mode print: anti-aliased rounded rectangles, a soft drop shadow and ARGB blending.
 * No Minecraft classes, so it is covered by JUnit.
 */
final class InstantPrint {

    private InstantPrint() {
    }

    /**
     * Signed distance from a point to a rounded rectangle: negative inside, 0 on the edge, positive outside.
     *
     * @param px     point x
     * @param py     point y
     * @param x      rectangle left
     * @param y      rectangle top
     * @param w      rectangle width
     * @param h      rectangle height
     * @param radius corner radius
     */
    static float distance(float px, float py, float x, float y, float w, float h, float radius) {
        float halfW = w / 2;
        float halfH = h / 2;
        float qx = Math.abs(px - x - halfW) - (halfW - radius);
        float qy = Math.abs(py - y - halfH) - (halfH - radius);
        float outX = Math.max(qx, 0);
        float outY = Math.max(qy, 0);
        return (float) Math.sqrt(outX * outX + outY * outY) + Math.min(Math.max(qx, qy), 0) - radius;
    }

    /** @return how much of a pixel whose centre is {@code distance} from an edge is covered (0..1, one-pixel anti-aliasing) */
    static float coverage(float distance) {
        return clamp(0.5F - distance);
    }

    /**
     * @return 1 on the edge, fading to 0 at {@code width} inside it; for hairlines and inner shading
     */
    static float edge(float distance, float width) {
        return clamp(1 + distance / width);
    }

    /** @return the shadow colour with its alpha falling off over {@code blur} outside the shape */
    static int shadow(float distance, float blur, int color, float maxAlpha) {
        float fade = 1 - clamp(distance / blur);
        return withAlpha(color, maxAlpha * fade * fade);
    }

    /** Linear blend of two ARGB colours, alpha included. */
    static int mix(int from, int to, float t) {
        int a = lerp(from >>> 24, to >>> 24, t);
        int r = lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** {@code top} with extra opacity {@code alpha} drawn over {@code bottom} (straight alpha, "source over"). */
    static int over(int top, float alpha, int bottom) {
        float topA = (top >>> 24) / 255F * clamp(alpha);
        float bottomA = (bottom >>> 24) / 255F;
        float outA = topA + bottomA * (1 - topA);
        if (outA <= 0) {
            return 0;
        }
        int r = blend(top >> 16 & 0xFF, topA, bottom >> 16 & 0xFF, bottomA, outA);
        int g = blend(top >> 8 & 0xFF, topA, bottom >> 8 & 0xFF, bottomA, outA);
        int b = blend(top & 0xFF, topA, bottom & 0xFF, bottomA, outA);
        return Math.round(outA * 255) << 24 | r << 16 | g << 8 | b;
    }

    static int withAlpha(int color, float alpha) {
        return Math.round(clamp(alpha) * 255) << 24 | color & 0xFFFFFF;
    }

    private static int blend(int top, float topA, int bottom, float bottomA, float outA) {
        return Math.round((top * topA + bottom * bottomA * (1 - topA)) / outA);
    }

    private static int lerp(int from, int to, float t) {
        return Math.round(from + (to - from) * clamp(t));
    }

    private static float clamp(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
