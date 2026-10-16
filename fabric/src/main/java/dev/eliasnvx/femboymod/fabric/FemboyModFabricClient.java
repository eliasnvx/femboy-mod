package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.photo.PhotoItemRenderer;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetOutfitLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetSpecies;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class FemboyModFabricClient implements ClientModInitializer {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        FemboyModClient.init();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("accessories")) {
            dev.eliasnvx.femboymod.compat.accessories.AccessoriesSlots.registerClient();
        }
        // Colorway tints are served by client/ItemTints as an ItemColor (1.21.1 has no item tint sources).
        // Printed photos: 1.21.1 has no special item models; the photo item model extends builtin/entity
        BuiltinItemRendererRegistry.INSTANCE.register(FemboyItems.PHOTO.get(), PhotoItemRenderer::render);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new CosmeticLayer(player, context.getModelSet()));
            }
            PetSpecies species = PetSpecies.forType(type);
            if (species != null) {
                helper.register(new PetOutfitLayer((RenderLayerParent) renderer, context.getModelSet(), species));
            }
        });
    }
}
