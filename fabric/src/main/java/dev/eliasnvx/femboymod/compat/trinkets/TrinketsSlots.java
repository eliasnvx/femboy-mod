package dev.eliasnvx.femboymod.compat.trinkets;

import dev.eliasnvx.femboymod.compat.ExternalSlots;
import dev.emi.trinkets.api.TrinketsApi;

/**
 * Trinkets (soft dependency): our cosmetics in Trinkets slots count as worn. Which items fit which slot comes from the
 * data pack ({@code data/trinkets/tags}, {@code data/trinkets/entities/femboymod.json}; see docs/compat.md).
 * Loaded only with Trinkets.
 */
public final class TrinketsSlots {

    private TrinketsSlots() {
    }

    public static void register() {
        ExternalSlots.register((player, action) -> TrinketsApi.getTrinketComponent(player)
                .ifPresent(component -> component.forEach((slot, stack) -> action.accept(stack))));
    }
}
