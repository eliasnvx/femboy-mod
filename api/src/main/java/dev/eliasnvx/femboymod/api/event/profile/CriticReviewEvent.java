package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server after the Fashion Critic reviewed a player's outfit. Not cancellable.
 *
 * @param player    the reviewed player
 * @param critic    the critic
 * @param dripTier  the player's Drip tier at review time
 * @param impressed whether the outfit passed (the critic gets weakened instead of the player)
 */
@ApiStatus.AvailableSince("0.1.0")
public record CriticReviewEvent(ServerPlayer player, Entity critic, int dripTier, boolean impressed) implements FemboyEvent {
}
