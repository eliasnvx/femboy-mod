package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.block.PlushShelfBlock;
import dev.eliasnvx.femboymod.block.PlushShelfBlockEntity;
import dev.eliasnvx.femboymod.block.PlushSquish;
import dev.eliasnvx.femboymod.item.PlushBlockItem;
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

/** Renders up to 3 plushies or trinkets sitting on a wall-mounted {@link PlushShelfBlock}. */
public final class PlushShelfRenderer implements BlockEntityRenderer<PlushShelfBlockEntity, PlushShelfRenderer.State> {

    private static final float SPACING = 0.30F;
    private static final float SHELF_TOP_Y = 0.76F;
    private static final float WALL_OFFSET_Z = -0.30F;
    private static final float ITEM_SCALE = 0.42F;
    private final ItemModelResolver itemModelResolver;

    public PlushShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = new ItemStackRenderState[PlushShelfBlockEntity.SLOTS];
        final boolean[] present = new boolean[PlushShelfBlockEntity.SLOTS];
        final boolean[] plush = new boolean[PlushShelfBlockEntity.SLOTS];
        Direction facing = Direction.NORTH;
        float scaleY = 1.0F;
        float scaleXZ = 1.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlushShelfBlockEntity shelf, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(shelf, state, partialTicks, camera, breakProgress);
        state.facing = shelf.getBlockState().getValue(PlushShelfBlock.FACING);
        float scaleY = PlushSquish.scaleY(shelf.squishKind(), shelf.squishAge(partialTicks));
        state.scaleY = scaleY;
        state.scaleXZ = PlushSquish.scaleXZ(scaleY);
        int seed = (int) shelf.getBlockPos().asLong();
        for (int slot = 0; slot < PlushShelfBlockEntity.SLOTS; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            state.present[slot] = !stack.isEmpty();
            state.plush[slot] = stack.getItem() instanceof PlushBlockItem;
            if (state.present[slot]) {
                if (state.items[slot] == null) {
                    state.items[slot] = new ItemStackRenderState();
                }
                itemModelResolver.updateForTopItem(state.items[slot], stack, ItemDisplayContext.FIXED, shelf.level(), shelf, seed + slot);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < PlushShelfBlockEntity.SLOTS; slot++) {
            if (!state.present[slot]) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5F, SHELF_TOP_Y, 0.5F);
            pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
            pose.translate((slot - 1) * SPACING, 0.0F, WALL_OFFSET_Z);
            if (state.plush[slot] && (state.scaleY != 1.0F || state.scaleXZ != 1.0F)) {
                pose.translate(0.0F, -0.14F, 0.0F);
                pose.scale(state.scaleXZ, state.scaleY, state.scaleXZ);
                pose.translate(0.0F, 0.14F, 0.0F);
            }
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            state.items[slot].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
