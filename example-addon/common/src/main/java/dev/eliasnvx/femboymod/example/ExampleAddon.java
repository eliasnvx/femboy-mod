package dev.eliasnvx.femboymod.example;

import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.RegisterFemboyAddon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference addon that exercises every public API extension point (SPEC §8.5).
 * Discovered via the "femboymod" entrypoint on Fabric and {@link RegisterFemboyAddon} on NeoForge.
 */
@RegisterFemboyAddon
public final class ExampleAddon implements FemboyAddon {

    public static final String MOD_ID = "femboymod_example";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize(FemboyApi api) {
        LOGGER.info("femboymod example addon initialized against API {}", api.apiVersion());
    }

    @Override
    public void onInitializeClient(FemboyClientApi api) {
        LOGGER.info("femboymod example addon client initialized");
    }
}
