package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.DripLevelChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.SetBonusEvent;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.server.level.ServerPlayer;

/** Feeds our own API events into the {@code femboymod:event} advancement trigger. */
public final class AdvancementHooks {

    private AdvancementHooks() {
    }

    public static void register(FemboyEventBus events) {
        events.addListener(CosmeticChangedEvent.class, e -> {
            if (e.entity() instanceof ServerPlayer player && e.current().is(FemboyItems.PROGRAMMING_SOCKS.get())) {
                FemboyTriggers.fire(player, FemboyTriggers.PROGRAMMING_SOCKS);
            }
        });
        events.addListener(DripLevelChangedEvent.class, e -> {
            if (e.player() instanceof ServerPlayer player) {
                FemboyTriggers.fire(player, FemboyTriggers.DRIP_TIER, e.current().tier());
            }
        });
        events.addListener(SetBonusEvent.Activate.class, e -> {
            if (e.player() instanceof ServerPlayer player) {
                FemboyTriggers.fire(player, FemboyTriggers.SET_BONUS + ":" + e.setId());
            }
        });
    }
}
