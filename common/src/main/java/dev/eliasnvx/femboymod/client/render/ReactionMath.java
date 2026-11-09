package dev.eliasnvx.femboymod.client.render;

import net.minecraft.util.Mth;

/** Pure math of reactive ears and tails (no game state), so it can be unit tested. */
public final class ReactionMath {

    private ReactionMath() {
    }

    /**
     * Minecraft yaw (degrees) of the direction (dx, dz): 0 = south (+z), 90 = west, turning right increases it.
     */
    public static float worldYaw(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
    }

    /**
     * Where the ears turn, in radians from the head's facing (negative = the wearer's left, like
     * {@code CosmeticMotion#turnSway}), limited to {@code maxTurn}.
     *
     * @param targetYaw world yaw toward what was noticed (degrees)
     * @param headYaw   world yaw the head faces (degrees)
     */
    public static float earYaw(float targetYaw, float headYaw, float maxTurn) {
        float relative = Mth.wrapDegrees(targetYaw - headYaw) * Mth.DEG_TO_RAD;
        return Mth.clamp(relative, -maxTurn, maxTurn);
    }

    /**
     * Moves {@code current} toward {@code target} with exponential smoothing; {@code riseRate} applies when the
     * value grows, {@code fallRate} when it shrinks (per tick, higher = faster). Frame-rate independent.
     */
    public static float approach(float current, float target, float riseRate, float fallRate, float dtTicks) {
        float rate = target > current ? riseRate : fallRate;
        float alpha = 1.0F - (float) Math.exp(-dtTicks * rate);
        return current + (target - current) * alpha;
    }

    /** How alert ears get for something at {@code distance} of {@code radius}: 1 up close, {@code far} at the edge. */
    public static float noticeStrength(double distance, double radius, float far) {
        if (radius <= 0) {
            return 0.0F;
        }
        float t = (float) Mth.clamp(distance / radius, 0.0, 1.0);
        return 1.0F - (1.0F - far) * t;
    }

    /** Droop from health: 0 at {@code start} of max health and above, 1 at {@code full} and below. */
    public static float lowHealthDroop(float health, float maxHealth, float start, float full) {
        if (maxHealth <= 0) {
            return 0.0F;
        }
        float ratio = health / maxHealth;
        return Mth.clamp((start - ratio) / (start - full), 0.0F, 1.0F);
    }
}
