package dev.eliasnvx.femboymod.api.event.cosmetic;

import dev.eliasnvx.femboymod.api.event.FemboyEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Set bonus lifecycle events, posted on the logical server. Not cancellable. */
public final class SetBonusEvent {

    private SetBonusEvent() {
    }

    /**
     * A set became complete.
     *
     * @param player the player
     * @param setId  id of the {@code femboymod:set_bonus} entry
     */
    public record Activate(Player player, ResourceLocation setId) implements FemboyEvent {
    }

    /**
     * A set stopped being complete (piece removed, logout, death).
     *
     * @param player the player
     * @param setId  id of the {@code femboymod:set_bonus} entry
     */
    public record Deactivate(Player player, ResourceLocation setId) implements FemboyEvent {
    }
}
