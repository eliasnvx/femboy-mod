package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server after a player wore a cosmetic item for the first time and it was added to
 * {@link dev.eliasnvx.femboymod.api.profile.FemboyProfileFields#COLLECTION}. Not cancellable.
 *
 * @param player    the player
 * @param item      the item id
 * @param collected collection size after the unlock
 */
@ApiStatus.AvailableSince("0.1.0")
public record CollectionUnlockEvent(ServerPlayer player, Identifier item, int collected) implements FemboyEvent {
}
