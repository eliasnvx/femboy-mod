package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.platform.fabric.PlatformHelperImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;

public final class FemboyModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformHelperImpl.init();
        FemboyMod.init();
        // Architectury 13 has no start-tracking event: sync a player's cosmetics when they come into view.
        EntityTrackingEvents.START_TRACKING.register(CosmeticsEvents::onStartTracking);
    }
}
