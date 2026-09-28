package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.profile.Profiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** S2C: synced profile fields of the receiving player (all of them on join, single changes afterwards). */
public record ProfileSyncPayload(CompoundTag values) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "profile_sync");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(values);
    }

    public static ProfileSyncPayload read(FriendlyByteBuf buf) {
        CompoundTag values = buf.readNbt();
        return new ProfileSyncPayload(values == null ? new CompoundTag() : values);
    }

    public static void handle(ProfileSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> PlatformHelper.setProfile(context.getPlayer(),
                PlatformHelper.getProfile(context.getPlayer()).merge(payload.values())));
    }

    public static void send(ServerPlayer player, CompoundTag values) {
        if (player.connection != null && FemboyNetwork.canReceive(player, ID)) {
            FemboyNetwork.sendToPlayer(player, new ProfileSyncPayload(values));
        }
    }

    public static void sendAll(ServerPlayer player) {
        send(player, Profiles.syncedValues(player));
    }
}
