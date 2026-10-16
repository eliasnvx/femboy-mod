package dev.eliasnvx.femboymod.client.render.entity;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RubberDuckRenderer extends MobRenderer<RubberDuck, RubberDuckModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "rubber_duck"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/rubber_duck.png");
    private static final float SHADOW = 0.25F;

    public RubberDuckRenderer(EntityRendererProvider.Context context) {
        super(context, new RubberDuckModel(context.bakeLayer(LAYER)), SHADOW);
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(LAYER, RubberDuckModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(RubberDuck duck) {
        return TEXTURE;
    }
}
