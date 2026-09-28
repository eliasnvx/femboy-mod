package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.entity.FashionCritic;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Humanoid on the player model: beret on the hat layer, sunglasses, turtleneck (texture in skin layout). */
public final class FashionCriticRenderer extends HumanoidMobRenderer<FashionCritic, HumanoidModel<FashionCritic>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/fashion_critic.png");
    private static final float SHADOW = 0.5F;

    public FashionCriticRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), SHADOW);
    }

    @Override
    public ResourceLocation getTextureLocation(FashionCritic critic) {
        return TEXTURE;
    }
}
