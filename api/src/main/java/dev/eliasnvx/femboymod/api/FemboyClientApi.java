package dev.eliasnvx.femboymod.api;

import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Client-only part of the Femboy Mod API (renderers, chat transformers, client events).
 *
 * <p>Passed to {@link FemboyAddon#onInitializeClient(FemboyClientApi)}. Never available on a
 * dedicated server; do not reference client-only Minecraft classes from common addon code.
 */
public interface FemboyClientApi {

    /**
     * Returns the client API instance.
     *
     * @return the client API
     * @throws IllegalStateException before client initialization or on a dedicated server
     */
    static FemboyClientApi get() {
        return dev.eliasnvx.femboymod.api.internal.FemboyClientApiHolder.get();
    }

    /**
     * Returns the common API instance.
     *
     * @return the common API, never {@code null}
     */
    FemboyApi common();

    /**
     * Returns the registry of cosmetic renderers, keyed by renderer id (by convention the item id).
     * Register during {@link FemboyAddon#onInitializeClient}.
     *
     * @return the renderer factory registry
     */
    ApiRegistry<CosmeticRenderer.Factory> cosmeticRenderers();

    /**
     * Returns motion values of the player behind a render state, for procedural animation in
     * {@code setupAnim}.
     *
     * @param state the player's render state
     * @return the motion values; neutral values if the player wears nothing
     */
    CosmeticMotion motion(AvatarRenderState state);
}
