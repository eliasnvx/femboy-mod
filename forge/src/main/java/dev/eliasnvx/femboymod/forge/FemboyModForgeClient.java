package dev.eliasnvx.femboymod.forge;

import dev.eliasnvx.femboymod.client.ConfigScreen;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup, called from {@link FemboyModForge} on the physical client. */
final class FemboyModForgeClient {

    private FemboyModForgeClient() {
    }

    static void init(FMLJavaModLoadingContext context) {
        FemboyModClient.init();
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
        // Item colorway tints come from client/ItemTints (an ItemColor); 1.20.1 has no item tint sources.
        context.getModEventBus().addListener((EntityRenderersEvent.AddLayers event) -> {
            for (String skin : event.getSkins()) {
                EntityRenderer<? extends Player> renderer = event.getSkin(skin);
                if (renderer instanceof PlayerRenderer playerRenderer) {
                    playerRenderer.addLayer(new CosmeticLayer(playerRenderer, event.getEntityModels()));
                }
            }
        });
    }
}
