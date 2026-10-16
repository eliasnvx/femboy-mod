package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.ColorwayTintSource;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import dev.eliasnvx.femboymod.client.ConfigScreen;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import dev.eliasnvx.femboymod.client.render.pet.PetOutfitLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetSpecies;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = FemboyMod.MOD_ID, dist = Dist.CLIENT)
public final class FemboyModNeoForgeClient {

    public FemboyModNeoForgeClient(IEventBus modBus, ModContainer container) {
        FemboyModClient.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) -> event.register(
                dev.eliasnvx.femboymod.client.photo.PhotoSpecialRenderer.ID, dev.eliasnvx.femboymod.client.photo.PhotoSpecialRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(ColorwayTintSource.ID, ColorwayTintSource.MAP_CODEC));
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerModelType skin : event.getSkins()) {
                AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
                if (renderer != null) {
                    renderer.addLayer(new CosmeticLayer(renderer, event.getEntityModels()));
                }
            }
            for (EntityType<?> type : PetSpecies.types()) {
                if (event.getRenderer(type) instanceof LivingEntityRenderer<?, ?, ?> renderer) {
                    addPetOutfitLayer(renderer, event.getEntityModels(), PetSpecies.forType(type));
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addPetOutfitLayer(LivingEntityRenderer renderer, net.minecraft.client.model.geom.EntityModelSet models, PetSpecies species) {
        renderer.addLayer(new PetOutfitLayer(renderer, models, species));
    }
}
