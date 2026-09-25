package dev.eliasnvx.femboymod.api.internal;

import dev.eliasnvx.femboymod.api.FemboyApi;
import org.jetbrains.annotations.ApiStatus;

/**
 * Holds the {@link FemboyApi} implementation installed by Femboy Mod. Not for addon use.
 */
@ApiStatus.Internal
public final class FemboyApiHolder {

    private static volatile FemboyApi instance;

    private FemboyApiHolder() {
    }

    /**
     * Returns the installed API instance.
     *
     * @return the API instance, never {@code null}
     * @throws IllegalStateException if Femboy Mod has not initialized yet
     */
    public static FemboyApi get() {
        FemboyApi api = instance;
        if (api == null) {
            throw new IllegalStateException(
                    "Femboy Mod API accessed before initialization. Use the FemboyApi passed to "
                            + "FemboyAddon#onInitialize instead of calling FemboyApi.get() from static initializers.");
        }
        return api;
    }

    /**
     * Installs the API implementation. Called exactly once by Femboy Mod.
     *
     * @param api the implementation, never {@code null}
     * @throws IllegalStateException if an implementation is already installed
     */
    public static void install(FemboyApi api) {
        if (api == null) {
            throw new IllegalArgumentException("api");
        }
        synchronized (FemboyApiHolder.class) {
            if (instance != null) {
                throw new IllegalStateException("Femboy Mod API is already installed");
            }
            instance = api;
        }
    }

    /** Clears the installed instance. For unit tests only. */
    @ApiStatus.Internal
    static void resetForTests() {
        instance = null;
    }
}
