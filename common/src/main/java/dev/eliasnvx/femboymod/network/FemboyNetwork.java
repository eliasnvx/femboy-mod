package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FemboyNetwork {

    private FemboyNetwork() {
    }

    public static void register() {
        registerS2C(CosmeticsSyncPayload.TYPE, CosmeticsSyncPayload.STREAM_CODEC, CosmeticsSyncPayload::handle);
        registerS2C(ProfileSyncPayload.TYPE, ProfileSyncPayload.STREAM_CODEC, ProfileSyncPayload::handle);
        registerS2C(EmotePayloads.Show.TYPE, EmotePayloads.Show.STREAM_CODEC, EmotePayloads.Show::handle);
        registerC2S(EmotePayloads.Play.TYPE, EmotePayloads.Play.STREAM_CODEC, EmotePayloads.Play::handle);
        registerC2S(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle);
        registerC2S(OpenCosmeticsMenuPayload.TYPE, OpenCosmeticsMenuPayload.STREAM_CODEC, OpenCosmeticsMenuPayload::handle);
        registerC2S(CycleArmorVisibilityPayload.TYPE, CycleArmorVisibilityPayload.STREAM_CODEC, CycleArmorVisibilityPayload::handle);
        registerC2S(ToggleCosmeticHiddenPayload.TYPE, ToggleCosmeticHiddenPayload.STREAM_CODEC, ToggleCosmeticHiddenPayload::handle);
        registerC2S(CreativeCosmeticSetPayload.TYPE, CreativeCosmeticSetPayload.STREAM_CODEC, CreativeCosmeticSetPayload::handle);
        registerC2S(CosmeticPanelClickPayload.TYPE, CosmeticPanelClickPayload.STREAM_CODEC, CosmeticPanelClickPayload::handle);
    }

    /** Architectury 13: C2S receivers are registered on both sides (the client only needs the type). */
    private static <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        NetworkManager.registerReceiver(NetworkManager.c2s(), type, codec, receiver);
    }

    /**
     * Architectury 13: an S2C receiver is registered on the client; a dedicated server only registers the
     * payload type. The handlers use common classes only, so referencing them on the server is safe.
     */
    private static <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), type, codec, receiver);
        } else {
            NetworkManager.registerS2CPayloadType(type, codec);
        }
    }
}
