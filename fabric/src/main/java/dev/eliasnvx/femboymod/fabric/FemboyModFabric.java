package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import net.fabricmc.api.ModInitializer;

public final class FemboyModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FemboyMod.init();
    }
}
