package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.world.entity.player.Player;

/**
 * Posted on the logical server when a player's Drip Level changes. Not cancellable.
 *
 * @param player   the player
 * @param previous old level
 * @param current  new level
 */
public record DripLevelChangedEvent(Player player, DripLevel previous, DripLevel current) implements FemboyEvent {
}
