package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.profile.Profiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** S2C: synced profile fields of the receiving player (all of them on join, single changes afterwards). */
public record ProfileSyncPayload(CompoundTag values) implements CustomPacketPayload {

    public static final Type<ProfileSyncPayload> TYPE = new Type<>(new ResourceLocation(FemboyMod.MOD_ID, "profile_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfileSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, ProfileSyncPayload::values, ProfileSyncPayload::new);

    @Override
    public Type<ProfileSyncPayload> type() {
        return TYPE;
    }

    public static void handle(ProfileSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> PlatformHelper.setProfile(context.getPlayer(),
                PlatformHelper.getProfile(context.getPlayer()).merge(payload.values())));
    }

    public static void send(ServerPlayer player, CompoundTag values) {
        if (player.connection != null && NetworkManager.canPlayerReceive(player, TYPE)) {
            NetworkManager.sendToPlayer(player, new ProfileSyncPayload(values));
        }
    }

    public static void sendAll(ServerPlayer player) {
        send(player, Profiles.syncedValues(player));
    }
}
