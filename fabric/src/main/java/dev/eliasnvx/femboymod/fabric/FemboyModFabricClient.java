package dev.eliasnvx.femboymod.fabric;

import dev.eliasnvx.femboymod.client.FemboyModClient;
import net.fabricmc.api.ClientModInitializer;

public final class FemboyModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FemboyModClient.init();
    }
}
