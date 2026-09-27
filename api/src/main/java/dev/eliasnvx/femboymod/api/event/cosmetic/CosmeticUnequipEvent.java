package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Posted on the logical server after an item was removed from a cosmetic slot (taken out,
 * replaced, or dropped on death). Not cancellable.
 *
 * @param entity  the former wearer
 * @param slot    the slot id
 * @param removed the removed item; do not modify
 */
public record CosmeticUnequipEvent(LivingEntity entity, ResourceLocation slot, ItemStack removed) implements FemboyEvent {
}
