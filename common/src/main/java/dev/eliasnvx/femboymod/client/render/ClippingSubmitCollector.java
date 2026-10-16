package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

/**
 * Forwards what an entity renderer submits, drawn only where it is seen through a window
 * ({@link ClippingVertexConsumer}): models, items and custom geometry (GeckoLib draws through it). Shadows, labels,
 * flames, leashes, blocks and particles are left out. Used for mirror reflections, so they never spill past the
 * glass. Models are set up and drawn at draw time, like vanilla's model feature renderer does.
 */
final class ClippingSubmitCollector implements SubmitNodeCollector {

    /** Draw-time helpers, render thread only. */
    private static final ClippingVertexConsumer CLIP = new ClippingVertexConsumer();
    private static final PoseStack POSE = new PoseStack();
    private static final QuadInstance QUAD = new QuadInstance();

    private final SubmitNodeCollector root;
    private final OrderedSubmitNodeCollector target;
    private final float[] window;
    private final List<RenderType> itemTypes = new ArrayList<>(2);

    /** @param window four planes, see {@link ClippingVertexConsumer#begin}; must stay unchanged until drawn */
    ClippingSubmitCollector(SubmitNodeCollector root, float[] window) {
        this(root, root, window);
    }

    private ClippingSubmitCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector target, float[] window) {
        this.root = root;
        this.target = target;
        this.window = window;
    }

    private static boolean quads(RenderType type) {
        return type.primitiveTopology() == PrimitiveTopology.QUADS;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new ClippingSubmitCollector(root, root.order(order), window);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay,
                                int tint, @Nullable UvMapping uvMapping, int outlineColor) {
        if (!quads(type)) {
            return;
        }
        float[] planes = window;
        target.submitCustomGeometry(pose, type, (drawPose, buffer) -> {
            VertexConsumer clipped = CLIP.begin(buffer, planes);
            model.setupAnim(state);
            POSE.last().set(drawPose);
            model.renderToBuffer(POSE, uvMapping == null ? clipped : uvMapping.wrap(clipped), light, overlay, tint);
            CLIP.end();
        });
    }

    @Override
    public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outlineColor, int[] tints,
                           ItemQuads quads, ItemStackRenderState.FoilType foil) {
        List<BakedQuad> all = quads.all();
        itemTypes.clear();
        for (BakedQuad quad : all) {
            RenderType type = itemType(quad.materialInfo(), foil);
            if (!itemTypes.contains(type)) {
                itemTypes.add(type);
            }
        }
        float[] planes = window;
        for (RenderType type : itemTypes) {
            if (!quads(type)) {
                continue;
            }
            target.submitCustomGeometry(pose, type, (drawPose, buffer) -> {
                VertexConsumer clipped = CLIP.begin(buffer, planes);
                QUAD.setLightCoords(light);
                QUAD.setOverlayCoords(overlay);
                for (BakedQuad quad : all) {
                    BakedQuad.MaterialInfo material = quad.materialInfo();
                    if (itemType(material, foil) == type) {
                        QUAD.setColor(material.isTinted() && material.tintIndex() >= 0 && material.tintIndex() < tints.length
                                ? tints[material.tintIndex()] : -1);
                        clipped.putBakedQuad(drawPose, quad, QUAD);
                    }
                }
                CLIP.end();
            });
        }
    }

    /** Like vanilla's item feature renderer; the special glint is drawn as the standard one. */
    private static RenderType itemType(BakedQuad.MaterialInfo material, ItemStackRenderState.FoilType foil) {
        return foil == ItemStackRenderState.FoilType.NONE ? material.itemRenderType() : material.itemGlintRenderType();
    }

    @Override
    public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
        if (!quads(type)) {
            return;
        }
        float[] planes = window;
        target.submitCustomGeometry(pose, type, (drawPose, buffer) -> {
            renderer.render(drawPose, CLIP.begin(buffer, planes));
            CLIP.end();
        });
    }

    // not part of a reflection

    @Override
    public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(PoseStack pose, @Nullable Vec3 attachment, int offset, Component name, boolean seeThrough,
                              int light, CameraRenderState camera) {
    }

    @Override
    public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow,
                           Font.DisplayMode mode, int light, int color, int background, int outline) {
    }

    @Override
    public void submitTextBackground(PoseStack pose, float x0, float y0, float x1, float y1, int color,
                                     Font.DisplayMode mode, int light) {
    }

    @Override
    public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack pose, EntityRenderState.LeashState leash) {
    }

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack pose, RenderType type, int light,
                                           int overlay, int tint, ModelFeatureRenderer.CrumblingOverlay overlayState) {
    }

    @Override
    public void submitMovingBlock(PoseStack pose, MovingBlockRenderState block, int light) {
    }

    @Override
    public void submitBlockModel(PoseStack pose, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light,
                                 int overlay, int outlineColor) {
    }

    @Override
    public void submitBreakingBlockModel(PoseStack pose, List<BlockStateModelPart> parts, int progress, boolean translucent) {
    }

    @Override
    public void submitShapeOutline(PoseStack pose, VoxelShape shape, RenderType type, int color, float width, boolean showAll) {
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group gizmos, CameraRenderState camera, boolean onTop) {
    }
}
