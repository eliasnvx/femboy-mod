package dev.eliasnvx.femboymod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.femboymod.client.render.ModelColors;
import dev.eliasnvx.femboymod.entity.StrayCat;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/**
 * Stray Cat: the vanilla cat model and renderer (collar, lying, sitting) with the vanilla white coat referenced
 * (not shipped) and tinted into a pastel shade per cat. 1.21.1 draws kittens with the adult coat, like vanilla.
 */
public final class StrayCatRenderer extends CatRenderer {

    private static final ResourceLocation WHITE = new ResourceLocation("textures/entity/cat/white.png");
    /** Pink, lavender, mint, peach, sky. */
    private static final int[] COATS = {
            ModelColors.opaque(0xFFD1E6), ModelColors.opaque(0xE3D1FF), ModelColors.opaque(0xCFF5E2),
            ModelColors.opaque(0xFFE0C8), ModelColors.opaque(0xD1ECFF)};
    private static final int NO_TINT = -1;

    private final TintedModel tinted;

    public StrayCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        tinted = new TintedModel(context.bakeLayer(ModelLayers.CAT));
        model = tinted; // 1.20.1 has no model tint hook: the body model applies the coat color itself
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        return WHITE;
    }

    @Override
    public void render(Cat cat, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        tinted.setTint(cat instanceof StrayCat stray ? COATS[Math.floorMod(stray.coat(), COATS.length)] : NO_TINT);
        super.render(cat, yaw, partialTick, pose, buffers, light);
    }

    /** Cat model that multiplies the coat tint into the color it is drawn with (collar layer untouched). */
    private static final class TintedModel extends CatModel<Cat> {
        private float tintRed = 1.0F;
        private float tintGreen = 1.0F;
        private float tintBlue = 1.0F;

        TintedModel(ModelPart root) {
            super(root);
        }

        void setTint(int argb) {
            tintRed = ModelColors.red(argb);
            tintGreen = ModelColors.green(argb);
            tintBlue = ModelColors.blue(argb);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                   float red, float green, float blue, float alpha) {
            super.renderToBuffer(pose, consumer, light, overlay, red * tintRed, green * tintGreen, blue * tintBlue, alpha);
        }
    }
}
