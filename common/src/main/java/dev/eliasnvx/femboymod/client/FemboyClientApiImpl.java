package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderStateAccess;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;

final class FemboyClientApiImpl implements FemboyClientApi {

    private final SimpleApiRegistry<CosmeticRenderer.Factory> renderers =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosmetic_renderer"));

    private final SimpleApiRegistry<dev.eliasnvx.femboymod.api.client.ChatTransformer> chatTransformers =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "chat_transformer"));

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
    public CosmeticMotion motion(AvatarRenderState state) {
        CosmeticRenderData data = ((CosmeticRenderStateAccess) state).femboymod$getCosmetics();
        return data == null ? CosmeticRenderData.STILL : data;
    }

    void freeze() {
        renderers.freeze();
        chatTransformers.freeze();
    }
}
