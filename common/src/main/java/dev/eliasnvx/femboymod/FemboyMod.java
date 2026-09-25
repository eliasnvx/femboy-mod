package dev.eliasnvx.femboymod;

import dev.eliasnvx.femboymod.addon.AddonLoader;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.internal.FemboyApiHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry point, called by each loader's main initializer. */
public final class FemboyMod {

    public static final String MOD_ID = FemboyApi.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized;

    private FemboyMod() {
    }

    public static synchronized void init() {
        if (initialized) {
            throw new IllegalStateException("FemboyMod.init() called twice");
        }
        initialized = true;

        FemboyApiImpl api = new FemboyApiImpl();
        FemboyApiHolder.install(api);

        AddonLoader.initCommon(api);

        LOGGER.info("femboymod initialized (api {}, {} addon(s))", api.apiVersion(), AddonLoader.addonCount());
    }
}
