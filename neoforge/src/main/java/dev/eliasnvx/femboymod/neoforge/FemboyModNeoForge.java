package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.platform.neoforge.PlatformHelperImpl;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(FemboyMod.MOD_ID)
public final class FemboyModNeoForge {

    public FemboyModNeoForge(IEventBus modBus) {
        PlatformHelperImpl.init(modBus);
        FemboyMod.init();
        FemboyGameTestsNeoForge.register(modBus);
        // Architectury 13 has no start-tracking event: sync cosmetics of players coming into view
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer watcher) {
                CosmeticsEvents.onStartTracking(event.getTarget(), watcher);
            }
        });
    }
}
