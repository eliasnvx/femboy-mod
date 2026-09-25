package dev.eliasnvx.femboymod.config;

import dev.architectury.platform.Platform;

/** Access to both config files. Common is loaded in FemboyMod.init, client in FemboyModClient.init. */
public final class FemboyConfig {

    private static final ConfigFile<CommonConfig> COMMON = new ConfigFile<>(
            Platform.getConfigFolder().resolve("femboymod-common.json"), CommonConfig.CODEC, CommonConfig.DEFAULTS);
    private static final ConfigFile<ClientConfig> CLIENT = new ConfigFile<>(
            Platform.getConfigFolder().resolve("femboymod-client.json"), ClientConfig.CODEC, ClientConfig.DEFAULTS);

    private FemboyConfig() {
    }

    public static CommonConfig common() {
        return COMMON.get();
    }

    public static ClientConfig client() {
        return CLIENT.get();
    }

    public static void loadCommon() {
        COMMON.load();
    }

    public static void loadClient() {
        CLIENT.load();
    }

    public static void setClient(ClientConfig config) {
        CLIENT.set(config);
    }
}
