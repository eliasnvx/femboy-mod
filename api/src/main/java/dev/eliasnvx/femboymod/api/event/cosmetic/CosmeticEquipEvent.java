package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Asks whether an item may be put into a cosmetic slot. Cancel to forbid it.
 *
 * <p>This is a <b>check</b>, not a notification: it may be posted several times for one
 * action and on both logical sides (the client uses it to predict slot highlights), so
 * listeners must be deterministic and free of side effects. React to actual changes with
 * {@link CosmeticChangedEvent}.
 */
public final class CosmeticEquipEvent implements CancellableEvent {

    private final LivingEntity entity;
    private final ResourceLocation slot;
    private final ItemStack stack;
    private boolean cancelled;

    /**
     * @param entity the wearer
     * @param slot   the target slot id
     * @param stack  the item (do not modify)
     */
    public CosmeticEquipEvent(LivingEntity entity, ResourceLocation slot, ItemStack stack) {
        this.entity = entity;
        this.slot = slot;
        this.stack = stack;
    }

    /** @return the wearer */
    public LivingEntity entity() {
        return entity;
    }

    /** @return the target slot id */
    public ResourceLocation slot() {
        return slot;
    }

    /** @return the item being equipped; do not modify */
    public ItemStack stack() {
        return stack;
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
