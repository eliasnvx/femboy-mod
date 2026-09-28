package dev.eliasnvx.femboymod.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.platform.forge.PlatformHelperImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(FemboyMod.MOD_ID)
public final class FemboyModForge {

    public FemboyModForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        // Architectury's DeferredRegister/events on Forge need the mod bus before any registration
        EventBuses.registerModEventBus(FemboyMod.MOD_ID, modBus);
        PlatformHelperImpl.init(modBus);
        FemboyMod.init();
        FemboyGameTestsForge.register(modBus);
        // Architectury 9 has no start-tracking event: sync cosmetics of players coming into view
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer watcher) {
                CosmeticsEvents.onStartTracking(event.getTarget(), watcher);
            }
        });
        // Forge 47's @Mod has no dist attribute: client-only setup lives in a class only loaded on the client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FemboyModForgeClient.init(context);
        }
    }
}
