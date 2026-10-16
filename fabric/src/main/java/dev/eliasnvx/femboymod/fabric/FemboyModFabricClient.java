package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.ColorwayTintSource;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import dev.eliasnvx.femboymod.client.render.pet.PetOutfitLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetSpecies;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class FemboyModFabricClient implements ClientModInitializer {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        FemboyModClient.init();
        // ID_MAPPER is widened by fabric-transitive-access-wideners
        ItemTintSources.ID_MAPPER.put(ColorwayTintSource.ID, ColorwayTintSource.MAP_CODEC);
        // also widened by fabric-transitive-access-wideners
        net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(
                dev.eliasnvx.femboymod.client.photo.PhotoSpecialRenderer.ID, dev.eliasnvx.femboymod.client.photo.PhotoSpecialRenderer.Unbaked.MAP_CODEC);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) {
                helper.register(new CosmeticLayer((RenderLayerParent<AvatarRenderState, PlayerModel>) avatar, context.getModelSet()));
            }
            PetSpecies species = PetSpecies.forType(type);
            if (species != null) {
                helper.register(new PetOutfitLayer((RenderLayerParent) renderer, context.getModelSet(), species));
            }
        });
    }
}
