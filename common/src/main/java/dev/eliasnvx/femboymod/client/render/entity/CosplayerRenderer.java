package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.entity.Cosplayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/** Wandering Cosplayer: player model in a pastel costume (texture in skin layout) with the mod's cat ears on top. */
public final class CosplayerRenderer extends HumanoidMobRenderer<Cosplayer, HumanoidModel<Cosplayer>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/cosplayer.png");
    private static final ResourceLocation FUR = new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/cosmetic/fur.png");
    private static final int EAR_COLOR = FastColor.ARGB32.opaque(0xFFB6DA);
    private static final float SHADOW = 0.5F;

    public CosplayerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), SHADOW);
        HumanoidModel<Cosplayer> ears = new HumanoidModel<>(context.bakeLayer(CosmeticModels.CAT_EARS));
        RenderType earType = RenderType.entityCutout(FUR);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Cosplayer cosplayer, float walkPos,
                               float walkSpeed, float partialTick, float ageInTicks, float headYaw, float headPitch) {
                if (cosplayer.isInvisible()) {
                    return;
                }
                getParentModel().copyPropertiesTo(ears);
                ears.renderToBuffer(pose, buffers.getBuffer(earType), light, OverlayTexture.NO_OVERLAY, EAR_COLOR);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Cosplayer cosplayer) {
        return TEXTURE;
    }
}
