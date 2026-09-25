package dev.eliasnvx.femboymod.api.internal;

import dev.eliasnvx.femboymod.api.FemboyClientApi;
import org.jetbrains.annotations.ApiStatus;

/** Holds the client API implementation. Not for addon use. */
@ApiStatus.Internal
public final class FemboyClientApiHolder {

    private static volatile FemboyClientApi instance;

    private FemboyClientApiHolder() {
    }

    /**
     * @return the client API
     * @throws IllegalStateException before client initialization or on a dedicated server
     */
    public static FemboyClientApi get() {
        FemboyClientApi api = instance;
        if (api == null) {
            throw new IllegalStateException("Femboy Mod client API is not available (not initialized yet, or not on a client)");
        }
        return api;
    }

    /**
     * @param api the implementation; installed once by Femboy Mod
     */
    public static void install(FemboyClientApi api) {
        instance = api;
    }
}
