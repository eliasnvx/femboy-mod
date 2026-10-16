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
        // slot mods (soft dependencies): their slots can hold our cosmetics
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            dev.eliasnvx.femboymod.compat.trinkets.TrinketsSlots.register();
        }
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("accessories")) {
            dev.eliasnvx.femboymod.compat.accessories.AccessoriesSlots.register();
        }
        // Architectury 9 has no start-tracking event: sync a player's cosmetics when they come into view.
        EntityTrackingEvents.START_TRACKING.register(CosmeticsEvents::onStartTracking);
    }
}
