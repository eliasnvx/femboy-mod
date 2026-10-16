package dev.eliasnvx.femboymod.api.event.social;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server when a player gives a headpat (sneak + right-click with an empty hand on another
 * player or a tamed pet), before anything happens. Listeners may change how long the target stays Cozy, or cancel
 * the headpat: then there are no hearts, no sound, no Cozy and no pet healing.
 */
@ApiStatus.AvailableSince("0.2.0")
public final class HeadpatEvent implements CancellableEvent {

    private final Player patter;
    private final LivingEntity target;
    private int cozyTicks;
    private boolean cancelled;

    /**
     * @param patter    the player giving the headpat
     * @param target    who gets it: a player or a tamed pet
     * @param cozyTicks how long the target will be Cozy (from the config)
     */
    public HeadpatEvent(Player patter, LivingEntity target, int cozyTicks) {
        this.patter = patter;
        this.target = target;
        this.cozyTicks = cozyTicks;
    }

    /** @return the player giving the headpat */
    public Player patter() {
        return patter;
    }

    /** @return the player or tamed pet being patted */
    public LivingEntity target() {
        return target;
    }

    /** @return how long the target stays Cozy, in ticks (0: no Cozy) */
    public int cozyTicks() {
        return cozyTicks;
    }

    /** @param cozyTicks how long the target stays Cozy, in ticks; 0 or less gives no Cozy */
    public void setCozyTicks(int cozyTicks) {
        this.cozyTicks = cozyTicks;
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
