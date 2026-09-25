package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;

/** Client entry point, called by each loader's client initializer after {@link FemboyMod#init()}. */
public final class FemboyModClient {

    private FemboyModClient() {
    }

    public static void init() {
        FemboyApi common = FemboyApi.get();
        FemboyClientApi clientApi = () -> common;
        AddonLoader.initClient(clientApi);
        FemboyMod.LOGGER.info("femboymod client initialized");
    }
}
