package dev.eliasnvx.femboymod.block;

import org.jetbrains.annotations.Nullable;

/**
 * Pure squash-and-stretch spring curve for plushies on placement and right-click.
 * Anchored at the bottom center of the block so the plush stays planted while compressing and bouncing back.
 */
public final class PlushSquish {

    private static final float MIN_SCALE_Y = 0.4F;
    private static final float MAX_SCALE_Y = 1.4F;
    private static final float MIN_SQRT_INPUT = 0.25F;

    /** Two intensities: a lively squish on right-click and a softer settle on placement. */
    public enum Kind {
        CLICK(12.0F, 0.65F, 2.8F, 1.5F),
        PLACE(10.0F, 0.42F, 2.8F, 1.5F);

        private final float durationTicks;
        private final float amplitude;
        private final float decay;
        private final float cycles;

        Kind(float durationTicks, float amplitude, float decay, float cycles) {
            this.durationTicks = durationTicks;
            this.amplitude = amplitude;
            this.decay = decay;
            this.cycles = cycles;
        }

        public float durationTicks() {
            return durationTicks;
        }
    }

    private PlushSquish() {
    }

    /** Vertical scale at {@code ageTicks} since the squish started; returns {@code 1.0} outside the active window. */
    public static float scaleY(@Nullable Kind kind, float ageTicks) {
        if (kind == null || ageTicks <= 0.0F || ageTicks >= kind.durationTicks) {
            return 1.0F;
        }
        float t = ageTicks / kind.durationTicks;
        double wave = Math.exp(-kind.decay * t) * (1.0 - t) * Math.sin(t * kind.cycles * 2.0 * Math.PI);
        float y = (float) (1.0 - kind.amplitude * wave);
        return Math.clamp(y, MIN_SCALE_Y, MAX_SCALE_Y);
    }

    /** Horizontal scale that roughly preserves volume ({@code scaleXZ^2 * scaleY == 1}). */
    public static float scaleXZ(float scaleY) {
        if (scaleY == 1.0F) {
            return 1.0F;
        }
        return (float) (1.0 / Math.sqrt(Math.max(MIN_SQRT_INPUT, scaleY)));
    }
}
