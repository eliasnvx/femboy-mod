package dev.eliasnvx.femboymod.api.client;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Collects reactions for one wearer, passed to {@link CosmeticReactionSource#collect}. Every amount is clamped
 * to 0..1; when several sources add the same reaction, the strongest one counts. The values show up, smoothed,
 * in {@link CosmeticMotion}.
 */
@ApiStatus.AvailableSince("0.3.0")
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface CosmeticReactions {

    /**
     * The ears turn toward this entity and stand up. Only the strongest noticed entity is kept.
     *
     * @param entity   what the ears heard
     * @param strength 0..1 how alert they get
     */
    void notice(Entity entity, float strength);

    /** @param amount 0..1 the ears stand up without turning anywhere */
    void perk(float amount);

    /** @param amount 0..1 the ears are pinned back */
    void flatten(float amount);

    /** @param amount 0..1 ears and tail droop */
    void droop(float amount);

    /** @param amount 0..1 the tail wags */
    void wag(float amount);

    /** @param amount 0..1 the tail puffs up */
    void puff(float amount);

    /** @param amount 0..1 ears and tail twitch nervously */
    void jitter(float amount);
}
