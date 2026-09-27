package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.block.ClothingRackBlock;
import dev.eliasnvx.femboymod.block.ClothingRackBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Items hang from the rack's bar, side by side, facing out of the rack. */
public final class ClothingRackRenderer implements BlockEntityRenderer<ClothingRackBlockEntity> {

    private static final float SPACING = 0.28F;
    private static final float HANG_Y = 0.62F;
    private static final float ITEM_SCALE = 0.42F;
    private final ItemRenderer itemRenderer;

    public ClothingRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ClothingRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = rack.getBlockState().getValue(ClothingRackBlock.FACING);
        int seed = (int) rack.getBlockPos().asLong();
        for (int slot = 0; slot < ClothingRackBlockEntity.SLOTS; slot++) {
            ItemStack stack = rack.getItems().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F, HANG_Y, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // items hang side by side along the bar, facing out of the rack
            pose.translate((slot - 1) * SPACING, 0.0F, 0.0F);
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    rack.getLevel(), seed + slot);
            pose.popPose();
        }
    }
}
