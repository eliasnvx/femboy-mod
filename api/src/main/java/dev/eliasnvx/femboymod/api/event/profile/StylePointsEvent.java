package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server before a player's Style Points change: earned (positive amount) or spent
 * (negative). Listeners may change the amount (for example a clan tax or a bonus) or cancel the change.
 * Spending is refused afterwards if the player cannot afford the (possibly changed) amount.
 *
 * <p>Built-in reasons: {@code femboymod:set_worn}, {@code femboymod:critic_passed},
 * {@code femboymod:collection}, {@code femboymod:rubber_duck}, {@code femboymod:coupon}.
 */
@ApiStatus.AvailableSince("0.1.0")
public final class StylePointsEvent implements CancellableEvent {

    private final ServerPlayer player;
    private final Identifier reason;
    private int amount;
    private boolean cancelled;

    /**
     * @param player the player
     * @param reason why the points change
     * @param amount positive to earn, negative to spend
     */
    public StylePointsEvent(ServerPlayer player, Identifier reason, int amount) {
        this.player = player;
        this.reason = reason;
        this.amount = amount;
    }

    /** @return the player */
    public ServerPlayer player() {
        return player;
    }

    /** @return why the points change */
    public Identifier reason() {
        return reason;
    }

    /** @return the change: positive when earned, negative when spent */
    public int amount() {
        return amount;
    }

    /** @param amount the new change; keep the sign of the original */
    public void setAmount(int amount) {
        this.amount = amount;
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
