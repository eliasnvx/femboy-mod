package dev.eliasnvx.femboymod.client.photo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Item renderer of a printed photo: an instant-print card with the picture, in the hand, in item frames, on the
 * ground and in inventories. 26.3 used a special item model; 1.20.1 gives the item a {@code builtin/entity} model
 * (with item/generated's display transforms) and draws it here, from Fabric's BuiltinItemRendererRegistry and
 * Forge's item extensions. Same space as a flat item model: 0..1, card at z 0.5.
 */
public final class PhotoItemRenderer {

    private static final RenderType CARD = RenderType.entityCutout(texture("photo_card"));
    private static final RenderType DEVELOPING = RenderType.entityCutout(texture("photo_developing"));

    /** Card and picture corners (the picture is square with a wide margin at the bottom, like an instant print). */
    private static final float CARD_X0 = 0.09F;
    private static final float CARD_X1 = 0.91F;
    private static final float CARD_Y0 = 0.0F;
    private static final float CARD_Y1 = 1.0F;
    private static final float PHOTO_X0 = 0.15F;
    private static final float PHOTO_X1 = 0.85F;
    private static final float PHOTO_Y0 = 0.24F;
    private static final float PHOTO_Y1 = 0.94F;
    private static final float Z = 0.5F;
    /** The picture sits a hair in front of the card, the card's back a hair behind it. */
    private static final float PHOTO_Z = Z + 0.002F;
    private static final float BACK_OFFSET = 0.001F;
    private static final int WHITE = 255;

    private PhotoItemRenderer() {
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(FemboyMod.MOD_ID, "textures/item/" + name + ".png");
    }

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                              int light, int overlay) {
        PoseStack.Pose last = pose.last();
        VertexConsumer card = buffers.getBuffer(CARD);
        quad(last, card, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, false);
        quad(last, card, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, true);
        PhotoData photo = FemboyComponents.PHOTO.get(stack);
        ResourceLocation picture = photo == null ? null : PhotoTextures.texture(photo.id());
        RenderType type = picture == null ? DEVELOPING : RenderType.entityCutout(picture);
        quad(last, buffers.getBuffer(type), PHOTO_X0, PHOTO_Y0, PHOTO_X1, PHOTO_Y1, PHOTO_Z, light, overlay, false);
    }

    /** One textured quad facing +z (or -z for the back), full texture. */
    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
                             int light, int overlay, boolean back) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        if (back) {
            float zb = z - BACK_OFFSET;
            vertex(matrix, normal, consumer, x1, y0, zb, 1, 1, light, overlay, -1);
            vertex(matrix, normal, consumer, x0, y0, zb, 0, 1, light, overlay, -1);
            vertex(matrix, normal, consumer, x0, y1, zb, 0, 0, light, overlay, -1);
            vertex(matrix, normal, consumer, x1, y1, zb, 1, 0, light, overlay, -1);
        } else {
            vertex(matrix, normal, consumer, x0, y0, z, 0, 1, light, overlay, 1);
            vertex(matrix, normal, consumer, x1, y0, z, 1, 1, light, overlay, 1);
            vertex(matrix, normal, consumer, x1, y1, z, 1, 0, light, overlay, 1);
            vertex(matrix, normal, consumer, x0, y1, z, 0, 0, light, overlay, 1);
        }
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
                               float u, float v, int light, int overlay, float normalZ) {
        consumer.vertex(matrix, x, y, z).color(WHITE, WHITE, WHITE, WHITE).uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(normal, 0.0F, 0.0F, normalZ).endVertex();
    }
}
