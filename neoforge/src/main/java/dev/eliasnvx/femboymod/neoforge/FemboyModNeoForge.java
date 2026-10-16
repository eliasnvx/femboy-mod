package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.compat.curios.CuriosSlots;
import dev.eliasnvx.femboymod.platform.neoforge.PlatformHelperImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(FemboyMod.MOD_ID)
public final class FemboyModNeoForge {

    public FemboyModNeoForge(IEventBus modBus) {
        PlatformHelperImpl.init(modBus);
        FemboyMod.init();
        // slot mod (soft dependency): its slots can hold our cosmetics
        if (ModList.get().isLoaded("curios")) {
            CuriosSlots.register();
        }
        FemboyGameTestsNeoForge.register(modBus);
    }
}
