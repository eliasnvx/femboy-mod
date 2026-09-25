package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.platform.neoforge.PlatformHelperImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FemboyMod.MOD_ID)
public final class FemboyModNeoForge {

    public FemboyModNeoForge(IEventBus modBus) {
        PlatformHelperImpl.init(modBus);
        FemboyMod.init();
        FemboyGameTestsNeoForge.register(modBus);
    }
}
