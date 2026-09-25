package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Posted on the logical server after the charms on a backpack changed. Not cancellable.
 *
 * @param player   the player who changed them
 * @param backpack the backpack; do not modify
 * @param charms   the new charms (immutable, may contain empty stacks for free slots)
 */
public record CharmsChangedEvent(Player player, ItemStack backpack, List<ItemStack> charms) implements FemboyEvent {
}
