package dev.eliasnvx.femboymod.forge;

import dev.eliasnvx.femboymod.client.ConfigScreen;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetOutfitLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetSpecies;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup, called from {@link FemboyModForge} on the physical client. */
final class FemboyModForgeClient {

    private FemboyModForgeClient() {
    }

    static void init(FMLJavaModLoadingContext context) {
        FemboyModClient.init();
        if (net.minecraftforge.fml.ModList.get().isLoaded("accessories")) {
            // after registration: at construction the item registry holds only vanilla items, so none of ours would match
            context.getModEventBus().addListener((FMLClientSetupEvent event) ->
                    event.enqueueWork(dev.eliasnvx.femboymod.compat.accessories.AccessoriesSlots::registerClient));
        }
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
            for (EntityType<?> type : PetSpecies.types()) {
                LivingEntityRenderer<?, ?> renderer = rendererOf(event, type);
                if (renderer != null) {
                    addPetOutfitLayer(renderer, event.getEntityModels(), PetSpecies.forType(type));
                }
            }
        });
    }

    /**
     * The renderer of a pet type, or null. Forge 47 types {@code getRenderer} by a living entity type; ours are given
     * as plain entity types (all of them living).
     */
    @SuppressWarnings("unchecked")
    private static LivingEntityRenderer<?, ?> rendererOf(EntityRenderersEvent.AddLayers event, EntityType<?> type) {
        return event.getRenderer((EntityType<LivingEntity>) type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addPetOutfitLayer(LivingEntityRenderer renderer, EntityModelSet models, PetSpecies species) {
        renderer.addLayer(new PetOutfitLayer(renderer, models, species));
    }
}
