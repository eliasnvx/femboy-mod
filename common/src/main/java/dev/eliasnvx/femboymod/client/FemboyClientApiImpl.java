package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

final class FemboyClientApiImpl implements FemboyClientApi {

    private final SimpleApiRegistry<CosmeticRenderer.Factory> renderers =
            new SimpleApiRegistry<>(new ResourceLocation(FemboyMod.MOD_ID, "cosmetic_renderer"));

    private final SimpleApiRegistry<dev.eliasnvx.femboymod.api.client.ChatTransformer> chatTransformers =
            new SimpleApiRegistry<>(new ResourceLocation(FemboyMod.MOD_ID, "chat_transformer"));

    @Override
    public ApiRegistry<dev.eliasnvx.femboymod.api.client.ChatTransformer> chatTransformers() {
        return chatTransformers;
    }

    @Override
    public FemboyApi common() {
        return FemboyApi.get();
    }

    @Override
    public ApiRegistry<CosmeticRenderer.Factory> cosmeticRenderers() {
        return renderers;
    }

    @Override
    public CosmeticMotion motion(LivingEntity entity) {
        return CosmeticRenderData.motion(entity);
    }

    void freeze() {
        renderers.freeze();
        chatTransformers.freeze();
    }
}
