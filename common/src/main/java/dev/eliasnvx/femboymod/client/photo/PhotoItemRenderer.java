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

/**
 * Printed photo item: an instant-print card with the picture, in the hand, in item frames, on the ground and in
 * inventories. 1.21.1 has no special item models, so this is a built-in item renderer (the item model extends
 * {@code builtin/entity} with the display transforms of {@code item/generated}); Fabric registers it with
 * {@code BuiltinItemRendererRegistry}, NeoForge through {@code IClientItemExtensions#getCustomRenderer}. Drawn in
 * the space of a flat item model (0..1, card at z 0.5), exactly like the 26.3 special model renderer.
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
    /** The picture sits a hair in front of the card. */
    private static final float PHOTO_Z = Z + 0.002F;
    /** The card's back face sits a hair behind its front. */
    private static final float BACK_OFFSET = 0.001F;
    private static final int WHITE = -1;

    private PhotoItemRenderer() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/item/" + name + ".png");
    }

    /** Draws the photo in item space; same parameters as a built-in item renderer. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                              int light, int overlay) {
        PoseStack.Pose last = pose.last();
        VertexConsumer card = buffers.getBuffer(CARD);
        quad(last, card, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, false);
        quad(last, card, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, true);
        PhotoData photo = stack.get(FemboyComponents.PHOTO.get());
        ResourceLocation picture = photo == null ? null : PhotoTextures.texture(photo.id());
        RenderType type = picture == null ? DEVELOPING : RenderType.entityCutout(picture);
        quad(last, buffers.getBuffer(type), PHOTO_X0, PHOTO_Y0, PHOTO_X1, PHOTO_Y1, PHOTO_Z, light, overlay, false);
    }

    /** One textured quad facing +z (or -z for the back), full texture. */
    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
                             int light, int overlay, boolean back) {
        if (back) {
            float zb = z - BACK_OFFSET;
            vertex(pose, consumer, x1, y0, zb, 1, 1, light, overlay, -1);
            vertex(pose, consumer, x0, y0, zb, 0, 1, light, overlay, -1);
            vertex(pose, consumer, x0, y1, zb, 0, 0, light, overlay, -1);
            vertex(pose, consumer, x1, y1, zb, 1, 0, light, overlay, -1);
        } else {
            vertex(pose, consumer, x0, y0, z, 0, 1, light, overlay, 1);
            vertex(pose, consumer, x1, y0, z, 1, 1, light, overlay, 1);
            vertex(pose, consumer, x1, y1, z, 1, 0, light, overlay, 1);
            vertex(pose, consumer, x0, y1, z, 0, 0, light, overlay, 1);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v,
                               int light, int overlay, float normalZ) {
        consumer.addVertex(pose, x, y, z).setColor(WHITE).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, 0.0F, 0.0F, normalZ);
    }
}
