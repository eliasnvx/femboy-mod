package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Posted on the logical server after the content of a cosmetic slot changed. Not cancellable.
 *
 * @param entity   the wearer
 * @param slot     the slot id
 * @param previous the previous item (may be empty); do not modify
 * @param current  the new item (may be empty); do not modify
 */
public record CosmeticChangedEvent(LivingEntity entity, Identifier slot, ItemStack previous, ItemStack current)
        implements FemboyEvent {
}
