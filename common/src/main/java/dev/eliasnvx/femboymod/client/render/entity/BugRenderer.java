package dev.eliasnvx.femboymod.client.render.entity;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.Bug;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

public final class BugRenderer extends MobRenderer<Bug, LivingEntityRenderState, BugModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "bug"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/bug.png");
    private static final float SHADOW = 0.3F;

    public BugRenderer(EntityRendererProvider.Context context) {
        super(context, new BugModel(context.bakeLayer(LAYER)), SHADOW);
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(LAYER, BugModel::createLayer);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
