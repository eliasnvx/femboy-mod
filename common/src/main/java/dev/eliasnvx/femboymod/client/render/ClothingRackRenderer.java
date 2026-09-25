package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.block.ClothingRackBlock;
import dev.eliasnvx.femboymod.block.ClothingRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Items hang from the rack's bar, side by side, facing out of the rack. */
public final class ClothingRackRenderer implements BlockEntityRenderer<ClothingRackBlockEntity, ClothingRackRenderer.State> {

    private static final float SPACING = 0.28F;
    private static final float HANG_Y = 0.62F;
    private static final float ITEM_SCALE = 0.42F;
    private final ItemModelResolver itemModelResolver;

    public ClothingRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[ClothingRackBlockEntity.SLOTS];
        final boolean[] present = new boolean[ClothingRackBlockEntity.SLOTS];
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ClothingRackBlockEntity rack, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTicks, camera, breakProgress);
        state.facing = rack.getBlockState().getValue(ClothingRackBlock.FACING);
        int seed = (int) rack.getBlockPos().asLong();
        for (int slot = 0; slot < ClothingRackBlockEntity.SLOTS; slot++) {
            ItemStack stack = rack.getItems().get(slot);
            state.present[slot] = !stack.isEmpty();
            if (state.present[slot]) {
                if (state.items[slot] == null) {
                    state.items[slot] = new ItemStackRenderState();
                }
                itemModelResolver.updateForTopItem(state.items[slot], stack, ItemDisplayContext.FIXED, rack.level(), rack, seed + slot);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < ClothingRackBlockEntity.SLOTS; slot++) {
            if (!state.present[slot]) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F, HANG_Y, 0.5F);
            pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
            // items hang side by side along the bar, facing out of the rack
            pose.translate((slot - 1) * SPACING, 0.0F, 0.0F);
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            state.items[slot].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
