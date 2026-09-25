package dev.eliasnvx.femboymod.api;

/**
 * Entry point for Femboy Mod addons.
 *
 * <p>Implementations must have a public no-argument constructor. They are discovered per loader:
 * <ul>
 *     <li><b>Fabric:</b> declare the class under the {@value #FABRIC_ENTRYPOINT} entrypoint in
 *     {@code fabric.mod.json}:
 *     <pre>{@code "entrypoints": { "femboymod": [ "com.example.MyAddon" ] }}</pre></li>
 *     <li><b>NeoForge:</b> annotate the class with {@link RegisterFemboyAddon}.</li>
 * </ul>
 * In a multi-loader (Architectury) addon, do both on the same common class.
 *
 * <p>Each addon is instantiated once. {@link #onInitialize(FemboyApi)} is called during Femboy Mod's
 * common initialization on both client and dedicated server. An exception thrown by one addon
 * is logged and does not prevent other addons from loading.
 */
public interface FemboyAddon {

    /** Name of the Fabric entrypoint used to discover addons: {@value}. */
    String FABRIC_ENTRYPOINT = "femboymod";

    /**
     * Called once during common initialization, on both physical sides.
     * Register your API extensions (slots, effects, event listeners) here.
     *
     * @param api the API instance, never {@code null}
     */
    void onInitialize(FemboyApi api);

    /**
     * Called once on the physical client, after {@link #onInitialize(FemboyApi)}.
     * Register client-only extensions (renderers, chat transformers) here.
     *
     * @param api the client API instance, never {@code null}
     */
    default void onInitializeClient(FemboyClientApi api) {
    }
}
