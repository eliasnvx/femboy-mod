package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.Util;

/**
 * Animation time for shimmering colorways ({@code ColorwayPattern.Shimmer}). Returns 0 (resting colors) when the
 * client turned RGB animations off.
 */
public final class ColorwayClock {

    private static final long MILLIS_PER_TICK = 50L;
    /** Wraps once an hour so the float keeps its precision. */
    private static final long WRAP_MILLIS = 3_600_000L;

    private ColorwayClock() {
    }

    public static float ticks() {
        if (!FemboyConfig.client().rgbAnimations()) {
            return 0.0F;
        }
        return (Util.getMillis() % WRAP_MILLIS) / (float) MILLIS_PER_TICK;
    }
}
