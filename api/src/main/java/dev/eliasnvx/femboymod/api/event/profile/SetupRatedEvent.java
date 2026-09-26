package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server when a player sitting in a Gamer Chair gets a setup rating: on sitting down and
 * whenever the rating changes. Not cancellable.
 *
 * @param player   the sitting player
 * @param chair    the chair position
 * @param stars    stars earned (0..{@code maxStars})
 * @param maxStars stars possible
 */
@ApiStatus.AvailableSince("0.1.0")
public record SetupRatedEvent(ServerPlayer player, BlockPos chair, int stars, int maxStars) implements FemboyEvent {
}
