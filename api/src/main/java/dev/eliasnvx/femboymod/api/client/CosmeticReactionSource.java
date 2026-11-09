package dev.eliasnvx.femboymod.api.client;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * A reason for reactive ears and tails to move. Called on the client a few times per second for every nearby
 * player who wears reactive cosmetics (the local player included); adds what should happen right now to
 * {@code reactions}. The mod combines all sources (the strongest value wins) and smooths the result over time,
 * so a source never animates anything itself.
 *
 * <p>Register in {@link dev.eliasnvx.femboymod.api.FemboyClientApi#reactionSources()} during
 * {@link dev.eliasnvx.femboymod.api.FemboyAddon#onInitializeClient}. Runs on the client thread; keep it cheap
 * (no world scans of your own if you can avoid them) and only use what the client knows: effects of other
 * players, for example, are not sent to other clients.
 */
@ApiStatus.AvailableSince("0.3.0")
@ApiStatus.Experimental
@FunctionalInterface
public interface CosmeticReactionSource {

    /**
     * @param wearer    the player whose ears and tail react (any player the client sees)
     * @param reactions where to add reactions; only valid during this call
     */
    void collect(Player wearer, CosmeticReactions reactions);
}
