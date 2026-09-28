package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.FastColor.ARGB32;

/**
 * Packed-color helpers for 1.20.1 model rendering. 1.21 takes one packed ARGB int in {@code ModelPart.render} and
 * {@code Model.renderToBuffer}; 1.20.1 takes four float channels. These keep call sites on packed colors.
 */
public final class ModelColors {

    private static final int OPAQUE_ALPHA = 0xFF000000;
    private static final float CHANNEL_MAX = 255.0F;

    private ModelColors() {
    }

    /** @return {@code rgb} with full alpha (1.21's {@code ARGB.opaque}) */
    public static int opaque(int rgb) {
        return rgb | OPAQUE_ALPHA;
    }

    public static void render(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int overlay, int argb) {
        part.render(pose, buffer, light, overlay, red(argb), green(argb), blue(argb), alpha(argb));
    }

    public static void render(Model model, PoseStack pose, VertexConsumer buffer, int light, int overlay, int argb) {
        model.renderToBuffer(pose, buffer, light, overlay, red(argb), green(argb), blue(argb), alpha(argb));
    }

    public static float red(int argb) {
        return ARGB32.red(argb) / CHANNEL_MAX;
    }

    public static float green(int argb) {
        return ARGB32.green(argb) / CHANNEL_MAX;
    }

    public static float blue(int argb) {
        return ARGB32.blue(argb) / CHANNEL_MAX;
    }

    public static float alpha(int argb) {
        return ARGB32.alpha(argb) / CHANNEL_MAX;
    }
}
