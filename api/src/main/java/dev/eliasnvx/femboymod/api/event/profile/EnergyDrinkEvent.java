package dev.eliasnvx.femboymod.api.event.profile;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Posted on the logical server after a player drank a Byte Energy can and its buffs were applied. Not
 * cancellable.
 *
 * @param player         the player
 * @param drink          the drink (before it was used up); do not modify
 * @param recentDrinks   cans drunk within the jitter window, this one included
 */
@ApiStatus.AvailableSince("0.1.0")
public record EnergyDrinkEvent(Player player, ItemStack drink, int recentDrinks) implements FemboyEvent {
}
