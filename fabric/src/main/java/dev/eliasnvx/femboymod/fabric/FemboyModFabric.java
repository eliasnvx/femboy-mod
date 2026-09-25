package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.platform.fabric.PlatformHelperImpl;
import net.fabricmc.api.ModInitializer;

public final class FemboyModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformHelperImpl.init();
        FemboyMod.init();
    }
}
