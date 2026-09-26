package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;

public final class FemboyNetwork {

    private FemboyNetwork() {
    }

    public static void register() {
        NetworkManager.registerS2C(CosmeticsSyncPayload.TYPE, CosmeticsSyncPayload.STREAM_CODEC, CosmeticsSyncPayload::handle);
        NetworkManager.registerC2S(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle);
        NetworkManager.registerC2S(OpenCosmeticsMenuPayload.TYPE, OpenCosmeticsMenuPayload.STREAM_CODEC, OpenCosmeticsMenuPayload::handle);
        NetworkManager.registerC2S(ToggleCosmeticHiddenPayload.TYPE, ToggleCosmeticHiddenPayload.STREAM_CODEC, ToggleCosmeticHiddenPayload::handle);
        NetworkManager.registerC2S(CreativeCosmeticSetPayload.TYPE, CreativeCosmeticSetPayload.STREAM_CODEC, CreativeCosmeticSetPayload::handle);
        NetworkManager.registerC2S(CosmeticPanelClickPayload.TYPE, CosmeticPanelClickPayload.STREAM_CODEC, CosmeticPanelClickPayload::handle);
    }
}
