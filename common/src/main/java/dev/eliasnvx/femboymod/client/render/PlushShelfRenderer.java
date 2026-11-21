package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.block.PlushShelfBlock;
import dev.eliasnvx.femboymod.block.PlushShelfBlockEntity;
import dev.eliasnvx.femboymod.block.PlushSquish;
import dev.eliasnvx.femboymod.item.PlushBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders up to 3 plushies or trinkets sitting on a wall-mounted {@link PlushShelfBlock}. */
public final class PlushShelfRenderer implements BlockEntityRenderer<PlushShelfBlockEntity> {

    private static final float SPACING = 0.30F;
    private static final float SHELF_TOP_Y = 0.76F;
    private static final float WALL_OFFSET_Z = -0.30F;
    private static final float ITEM_SCALE = 0.42F;
    private final ItemRenderer itemRenderer;

    public PlushShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PlushShelfBlockEntity shelf, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = shelf.getBlockState().getValue(PlushShelfBlock.FACING);
        float scaleY = PlushSquish.scaleY(shelf.squishKind(), shelf.squishAge(partialTick));
        float scaleXZ = PlushSquish.scaleXZ(scaleY);
        int seed = (int) shelf.getBlockPos().asLong();
        for (int slot = 0; slot < PlushShelfBlockEntity.SLOTS; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F, SHELF_TOP_Y, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate((slot - 1) * SPACING, 0.0F, WALL_OFFSET_Z);
            if (stack.getItem() instanceof PlushBlockItem && (scaleY != 1.0F || scaleXZ != 1.0F)) {
                pose.translate(0.0F, -0.14F, 0.0F);
                pose.scale(scaleXZ, scaleY, scaleXZ);
                pose.translate(0.0F, 0.14F, 0.0F);
            }
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    shelf.getLevel(), seed + slot);
            pose.popPose();
        }
    }
}
