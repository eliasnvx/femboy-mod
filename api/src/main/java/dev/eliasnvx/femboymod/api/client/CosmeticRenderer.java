package dev.eliasnvx.femboymod.api.client;

import net.minecraft.client.model.geom.EntityModelSet;

/**
 * Draws one worn cosmetic on a player. Registered as a {@link Factory} in
 * {@link dev.eliasnvx.femboymod.api.FemboyClientApi#cosmeticRenderers()} under the renderer id an item's
 * {@link dev.eliasnvx.femboymod.api.cosmetic.Cosmetic} points to (by default the item id).
 *
 * <p>Called on the render thread from the player render layer, once per frame per worn item. Rendering
 * is deferred in 26.x: submit a {@code Model} together with the render state and pose it in the model's
 * own {@code setupAnim}, so that shared model instances are posed correctly for every player.
 * Implementations must not allocate per call.
 */
@FunctionalInterface
public interface CosmeticRenderer {

    /**
     * Submits the cosmetic for rendering.
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
