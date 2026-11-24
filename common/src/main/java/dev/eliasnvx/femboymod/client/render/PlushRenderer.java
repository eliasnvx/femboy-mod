package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.block.PlushBlock;
import dev.eliasnvx.femboymod.block.PlushBlockEntity;
import dev.eliasnvx.femboymod.block.PlushSquish;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Renders plushie block models with a bottom-anchored squash-and-stretch spring when placed or clicked. */
public final class PlushRenderer implements BlockEntityRenderer<PlushBlockEntity, PlushRenderer.State> {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public PlushRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState model = new BlockModelRenderState();
        float scaleY = 1.0F;
        float scaleXZ = 1.0F;
        float yOffset = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlushBlockEntity plush, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plush, state, partialTicks, camera, breakProgress);
        blockModelResolver.update(state.model, plush.getBlockState(), DISPLAY_CONTEXT);
        float scaleY = PlushSquish.scaleY(plush.squishKind(), plush.squishAge(partialTicks));
        state.scaleY = scaleY;
        state.scaleXZ = PlushSquish.scaleXZ(scaleY);
        state.yOffset = (float) PlushBlock.restingYOffset(plush.getLevel(), plush.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.scaleY == 1.0F && state.scaleXZ == 1.0F && state.yOffset == 0.0F) {
            state.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            return;
        }
        pose.pushPose();
        if (state.yOffset != 0.0F) {
            pose.translate(0.0F, state.yOffset, 0.0F);
        }
        if (state.scaleY != 1.0F || state.scaleXZ != 1.0F) {
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.scale(state.scaleXZ, state.scaleY, state.scaleXZ);
            pose.translate(-0.5F, 0.0F, -0.5F);
        }
        state.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
