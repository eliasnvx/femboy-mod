package dev.eliasnvx.femboymod.client.photo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.photo.PhotoData;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jetbrains.annotations.Nullable;

/**
 * Item model of a printed photo ({@code "type": "femboymod:photo"}): an instant-print card with the picture, in the
 * hand, in item frames, on the ground and in inventories. In the space of a flat item model (0..1, card at z 0.5).
 */
public final class PhotoSpecialRenderer implements SpecialModelRenderer<UUID> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "photo");
    private static final RenderType CARD = RenderTypes.entityCutout(texture("photo_card"));
    private static final RenderType DEVELOPING = RenderTypes.entityCutout(texture("photo_developing"));

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

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/item/" + name + ".png");
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<UUID> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<UUID> bake(BakingContext context) {
            return new PhotoSpecialRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    @Override
    public @Nullable UUID extractArgument(ItemStack stack) {
        PhotoData photo = stack.get(FemboyComponents.PHOTO.get());
        return photo == null ? null : photo.id();
    }

    @Override
    public void submit(@Nullable UUID id, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        collector.submitCustomGeometry(pose, CARD, (entry, consumer) -> {
            quad(entry, consumer, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, false);
            quad(entry, consumer, CARD_X0, CARD_Y0, CARD_X1, CARD_Y1, Z, light, overlay, true);
        });
        Identifier picture = id == null ? null : PhotoTextures.texture(id);
        RenderType type = picture == null ? DEVELOPING : RenderTypes.entityCutout(picture);
        collector.submitCustomGeometry(pose, type, (entry, consumer) ->
                quad(entry, consumer, PHOTO_X0, PHOTO_Y0, PHOTO_X1, PHOTO_Y1, PHOTO_Z, light, overlay, false));
    }

    /** One textured quad facing +z (or -z for the back), full texture. */
    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
                             int light, int overlay, boolean back) {
        if (back) {
            float zb = z - 0.001F;
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
        consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(CARD_X0, CARD_Y0, Z - 0.001F));
        output.accept(new Vector3f(CARD_X1, CARD_Y1, PHOTO_Z));
    }
}
