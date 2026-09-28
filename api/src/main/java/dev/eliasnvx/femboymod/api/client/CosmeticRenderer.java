package dev.eliasnvx.femboymod.api.client;

import net.minecraft.client.model.geom.EntityModelSet;

/**
 * Draws one worn cosmetic on a player. Registered as a {@link Factory} in
 * {@link dev.eliasnvx.femboymod.api.FemboyClientApi#cosmeticRenderers()} under the renderer id an item's
 * {@link dev.eliasnvx.femboymod.api.cosmetic.Cosmetic} points to (by default the item id).
 *
 * <p>Called on the render thread from the player render layer, once per frame per worn item. Rendering
 * is immediate: draw right away into {@link CosmeticRenderContext#bufferSource()} with
 * {@link CosmeticRenderContext#poseStack()} (already in player model space). The wearer's model
 * ({@link CosmeticRenderContext#parentModel()}) is posed for this entity during this call only, so copy or
 * follow its parts right before drawing (for example {@code parentModel().body.translateAndRotate(pose)}, or
 * {@code HumanoidModel#copyPropertiesTo} onto your own shared model). Push and pop the pose stack yourself.
 * Implementations must not allocate per call (cache {@code RenderType}s in fields).
 */
@FunctionalInterface
public interface CosmeticRenderer {

    /**
     * Draws the cosmetic now.
     *
     * @param context what to draw and where; only valid during this call
     */
    void submit(CosmeticRenderContext context);

    /** Creates a renderer when entity renderers are (re)built; bake your model layers here. */
    @FunctionalInterface
    interface Factory {
        /**
         * @param models the baked entity model set
         * @return the renderer
         */
        CosmeticRenderer create(EntityModelSet models);
    }
}
