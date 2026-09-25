package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.CancellableEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Posted on the logical server before a backpack menu opens. Cancel to prevent opening. */
public final class BackpackOpenEvent implements CancellableEvent {

    private final Player player;
    private final ItemStack backpack;
    private final boolean worn;
    private boolean cancelled;

    /**
     * @param player   the player opening it
     * @param backpack the backpack (do not modify)
     * @param worn     true if opened from the back slot, false if from the hand
     */
    public BackpackOpenEvent(Player player, ItemStack backpack, boolean worn) {
        this.player = player;
        this.backpack = backpack;
        this.worn = worn;
    }

    /** @return the player */
    public Player player() {
        return player;
    }

    /** @return the backpack; do not modify */
    public ItemStack backpack() {
        return backpack;
    }

    /** @return whether it is opened from the back slot */
    public boolean worn() {
        return worn;
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
