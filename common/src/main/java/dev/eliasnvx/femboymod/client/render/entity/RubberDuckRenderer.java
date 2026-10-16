package dev.eliasnvx.femboymod.client.render.entity;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class RubberDuckRenderer extends MobRenderer<RubberDuck, LivingEntityRenderState, RubberDuckModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "rubber_duck"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/rubber_duck.png");
    private static final float SHADOW = 0.25F;

    public RubberDuckRenderer(EntityRendererProvider.Context context) {
        super(context, new RubberDuckModel(context.bakeLayer(LAYER)), SHADOW);
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(LAYER, RubberDuckModel::createLayer);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
