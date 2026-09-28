package dev.eliasnvx.femboymod.forge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.ConfigScreen;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FemboyMod.MOD_ID, dist = Dist.CLIENT)
public final class FemboyModForgeClient {

    public FemboyModForgeClient(IEventBus modBus, ModContainer container) {
        FemboyModClient.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
        // Item colorway tints come from client/ItemTints (an ItemColor); 1.21.1 has no item tint sources.
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new CosmeticLayer(renderer, event.getEntityModels()));
                }
            }
        });
    }
}
