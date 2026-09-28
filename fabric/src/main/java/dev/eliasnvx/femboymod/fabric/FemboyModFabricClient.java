package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class FemboyModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FemboyModClient.init();
        // Colorway tints are served by client/ItemTints as an ItemColor (1.21.1 has no item tint sources).
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new CosmeticLayer(player, context.getModelSet()));
            }
        });
    }
}
