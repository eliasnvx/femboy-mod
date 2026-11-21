package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.block.PlushBlockEntity;
import dev.eliasnvx.femboymod.block.PlushSquish;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

/** Renders plushie block models with a bottom-anchored squash-and-stretch spring when placed or clicked. */
public final class PlushRenderer implements BlockEntityRenderer<PlushBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public PlushRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(PlushBlockEntity plush, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = plush.getBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);
        float scaleY = PlushSquish.scaleY(plush.squishKind(), plush.squishAge(partialTick));
        float scaleXZ = PlushSquish.scaleXZ(scaleY);
        pose.pushPose();
        if (scaleY != 1.0F || scaleXZ != 1.0F) {
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.scale(scaleXZ, scaleY, scaleXZ);
            pose.translate(-0.5F, 0.0F, -0.5F);
        }
        blockRenderer.getModelRenderer().renderModel(
                pose.last(),
                buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state,
                model,
                1.0F, 1.0F, 1.0F,
                light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
