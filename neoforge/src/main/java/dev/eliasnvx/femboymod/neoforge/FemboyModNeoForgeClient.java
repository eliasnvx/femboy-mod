package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = FemboyMod.MOD_ID, dist = Dist.CLIENT)
public final class FemboyModNeoForgeClient {

    public FemboyModNeoForgeClient() {
        FemboyModClient.init();
    }
}
