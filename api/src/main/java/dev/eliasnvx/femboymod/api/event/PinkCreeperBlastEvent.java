package dev.eliasnvx.femboymod.api.event;

import net.minecraft.world.entity.Entity;

/**
 * Posted on the logical server when a Pink Creeper bursts into confetti (SPEC §5.4). Listeners may
 * change the numbers or cancel the burst (the creeper still disappears). Blocks are never damaged.
 */
public final class PinkCreeperBlastEvent implements CancellableEvent {

    private final Entity creeper;
    private float radius;
    private float knockback;
    private float playerDamage;
    private float mobDamage;
    private boolean cancelled;

    /**
     * @param creeper      the creeper
     * @param radius       blast radius in blocks
     * @param knockback    knockback strength
     * @param playerDamage damage to players
     * @param mobDamage    damage to other mobs
     */
    public PinkCreeperBlastEvent(Entity creeper, float radius, float knockback, float playerDamage, float mobDamage) {
        this.creeper = creeper;
        this.radius = radius;
        this.knockback = knockback;
        this.playerDamage = playerDamage;
        this.mobDamage = mobDamage;
    }

    /** @return the creeper */
    public Entity creeper() {
        return creeper;
    }

    /** @return blast radius */
    public float radius() {
        return radius;
    }

    /** @param radius new radius */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /** @return knockback strength */
    public float knockback() {
        return knockback;
    }

    /** @param knockback new knockback strength */
    public void setKnockback(float knockback) {
        this.knockback = knockback;
    }

    /** @return damage to players */
    public float playerDamage() {
        return playerDamage;
    }

    /** @param playerDamage new damage to players */
    public void setPlayerDamage(float playerDamage) {
        this.playerDamage = playerDamage;
    }

    /** @return damage to other mobs */
    public float mobDamage() {
        return mobDamage;
    }

    /** @param mobDamage new damage to other mobs */
    public void setMobDamage(float mobDamage) {
        this.mobDamage = mobDamage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
