package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;

public final class FemboyNetwork {

    private FemboyNetwork() {
    }

    public static void register() {
        NetworkManager.registerS2C(CosmeticsSyncPayload.TYPE, CosmeticsSyncPayload.STREAM_CODEC, CosmeticsSyncPayload::handle);
        NetworkManager.registerS2C(ProfileSyncPayload.TYPE, ProfileSyncPayload.STREAM_CODEC, ProfileSyncPayload::handle);
        NetworkManager.registerS2C(EmotePayloads.Show.TYPE, EmotePayloads.Show.STREAM_CODEC, EmotePayloads.Show::handle);
        NetworkManager.registerC2S(EmotePayloads.Play.TYPE, EmotePayloads.Play.STREAM_CODEC, EmotePayloads.Play::handle);
        NetworkManager.registerC2S(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle);
        NetworkManager.registerC2S(OpenCosmeticsMenuPayload.TYPE, OpenCosmeticsMenuPayload.STREAM_CODEC, OpenCosmeticsMenuPayload::handle);
        NetworkManager.registerC2S(CycleArmorVisibilityPayload.TYPE, CycleArmorVisibilityPayload.STREAM_CODEC, CycleArmorVisibilityPayload::handle);
        NetworkManager.registerC2S(ToggleCosmeticHiddenPayload.TYPE, ToggleCosmeticHiddenPayload.STREAM_CODEC, ToggleCosmeticHiddenPayload::handle);
        NetworkManager.registerC2S(CreativeCosmeticSetPayload.TYPE, CreativeCosmeticSetPayload.STREAM_CODEC, CreativeCosmeticSetPayload::handle);
        NetworkManager.registerC2S(CosmeticPanelClickPayload.TYPE, CosmeticPanelClickPayload.STREAM_CODEC, CosmeticPanelClickPayload::handle);
    }
}
