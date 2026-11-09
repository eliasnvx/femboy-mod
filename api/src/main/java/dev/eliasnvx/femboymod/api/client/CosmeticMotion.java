package dev.eliasnvx.femboymod.api.client;

import org.jetbrains.annotations.ApiStatus;

/**
 * Smoothed, per-player motion values for procedural animation (tails, skirts, ears). Stable for the
 * player across frames; obtain it in a model's {@code setupAnim} via
 * {@link dev.eliasnvx.femboymod.api.FemboyClientApi#motion}.
 *
 * <p>The reaction values (ears and tails reacting to what happens around the wearer) are 0 when the
 * wearer has no reactive cosmetics or the player turned reactions off. Reasons to react come from
 * {@link CosmeticReactionSource}s.
 */
public interface CosmeticMotion {

    /** @return sideways swing in radians caused by body turning (negative = left) */
    float turnSway();

    /** @return 0..1 how fast the player is walking, smoothed */
    float walkAmount();

    /** @return a per-player phase offset in radians, so idle animations of different players do not sync */
    float phase();

    /** @return 0..1 how alert the ears are: standing up and turned toward {@link #earYaw()} */
    @ApiStatus.AvailableSince("0.3.0")
    default float earAlert() {
        return 0.0F;
    }

    /**
     * @return radians from the wearer's facing to what the ears noticed (negative = left), already limited
     *         to how far ears can turn
     */
    @ApiStatus.AvailableSince("0.3.0")
    default float earYaw() {
        return 0.0F;
    }

    /** @return 0..1 how flat the ears are pinned back (fear, getting hurt, being underwater) */
    @ApiStatus.AvailableSince("0.3.0")
    default float earFlatten() {
        return 0.0F;
    }

    /** @return 0..1 how much ears and tail droop (low health, sleeping) */
    @ApiStatus.AvailableSince("0.3.0")
    default float droop() {
        return 0.0F;
    }

    /** @return 0..1 how happily the tail wags */
    @ApiStatus.AvailableSince("0.3.0")
    default float tailWag() {
        return 0.0F;
    }

    /** @return 0..1 how puffed up (startled) the tail is */
    @ApiStatus.AvailableSince("0.3.0")
    default float tailPuff() {
        return 0.0F;
    }

    /** @return 0..1 nervous twitching, e.g. after energy drinks; keep it a small motion, never a flash */
    @ApiStatus.AvailableSince("0.3.0")
    default float jitter() {
        return 0.0F;
    }
}
