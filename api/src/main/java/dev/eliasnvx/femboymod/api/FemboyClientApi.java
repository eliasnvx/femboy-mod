package dev.eliasnvx.femboymod.api;

/**
 * Client-only part of the Femboy Mod API (renderers, chat transformers, client events).
 *
 * <p>Passed to {@link FemboyAddon#onInitializeClient(FemboyClientApi)}. Never available on a
 * dedicated server; do not reference client-only Minecraft classes from common addon code.
 */
public interface FemboyClientApi {

    /**
     * Returns the common API instance.
     *
     * @return the common API, never {@code null}
     */
    FemboyApi common();
}
