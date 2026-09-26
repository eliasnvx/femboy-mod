package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.entity.Cosplayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Wandering Cosplayer: player model in a pastel costume (texture in skin layout) with the mod's cat ears on top. */
public final class CosplayerRenderer extends HumanoidMobRenderer<Cosplayer, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosplayer.png");
    private static final Identifier FUR = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/fur.png");
    private static final int EAR_COLOR = 0xFFB6DA;
    private static final float SHADOW = 0.5F;

    public CosplayerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), SHADOW);
        HumanoidModel<HumanoidRenderState> ears = new HumanoidModel<>(context.bakeLayer(CosmeticModels.CAT_EARS));
        RenderType earType = RenderTypes.entityCutout(FUR);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot, float xRot) {
                collector.submitModel(ears, state, pose, earType, light, OverlayTexture.NO_OVERLAY, ARGB.opaque(EAR_COLOR), null,
                        state.outlineColor);
            }
        });
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
