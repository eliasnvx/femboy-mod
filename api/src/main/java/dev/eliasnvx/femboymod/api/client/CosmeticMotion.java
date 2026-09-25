package dev.eliasnvx.femboymod.api.client;

/**
 * Smoothed, per-player motion values for procedural animation (tails, skirts, ears). Stable for the
 * player across frames; obtain it in a model's {@code setupAnim} via
 * {@link dev.eliasnvx.femboymod.api.FemboyClientApi#motion}.
 */
public interface CosmeticMotion {

    /** @return sideways swing in radians caused by body turning (negative = left) */
    float turnSway();

    /** @return 0..1 how fast the player is walking, smoothed */
    float walkAmount();

    /** @return a per-player phase offset in radians, so idle animations of different players do not sync */
    float phase();
}
