package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class FemboyNetwork {

    private FemboyNetwork() {
    }

    public static void register() {
        registerS2C(CosmeticsSyncPayload.ID, CosmeticsSyncPayload::read, CosmeticsSyncPayload::handle);
        registerS2C(ProfileSyncPayload.ID, ProfileSyncPayload::read, ProfileSyncPayload::handle);
        registerS2C(EmotePayloads.Show.ID, EmotePayloads.Show::read, EmotePayloads.Show::handle);
        registerC2S(EmotePayloads.Play.ID, EmotePayloads.Play::read, EmotePayloads.Play::handle);
        registerC2S(OpenBackpackPayload.ID, OpenBackpackPayload::read, OpenBackpackPayload::handle);
        registerC2S(OpenCosmeticsMenuPayload.ID, OpenCosmeticsMenuPayload::read, OpenCosmeticsMenuPayload::handle);
        registerC2S(CycleArmorVisibilityPayload.ID, CycleArmorVisibilityPayload::read, CycleArmorVisibilityPayload::handle);
        registerC2S(ToggleCosmeticHiddenPayload.ID, ToggleCosmeticHiddenPayload::read, ToggleCosmeticHiddenPayload::handle);
        registerC2S(CreativeCosmeticSetPayload.ID, CreativeCosmeticSetPayload::read, CreativeCosmeticSetPayload::handle);
        registerC2S(CosmeticPanelClickPayload.ID, CosmeticPanelClickPayload::read, CosmeticPanelClickPayload::handle);
    }

    /** Client only: sends {@code packet} to the server. */
    public static void sendToServer(FemboyPacket packet) {
        NetworkManager.sendToServer(packet.id(), encode(packet));
    }

    /** Sends {@code packet} to one player; the caller checks {@link #canReceive} for optional messages. */
    public static void sendToPlayer(ServerPlayer player, FemboyPacket packet) {
        if (isConnected(player)) {
            NetworkManager.sendToPlayer(player, packet.id(), encode(packet));
        }
    }

    /** Encodes {@code packet} once and sends the same bytes to every player in {@code players}. */
    public static void sendToPlayers(Iterable<ServerPlayer> players, FemboyPacket packet) {
        java.util.List<ServerPlayer> connected = new java.util.ArrayList<>();
        players.forEach(player -> {
            if (isConnected(player)) {
                connected.add(player);
            }
        });
        if (!connected.isEmpty()) {
            NetworkManager.sendToPlayers(connected, packet.id(), encode(packet));
        }
    }

    /** GameTest mock players have a connection without a channel; Forge throws when a packet is sent to them. */
    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null
                && ((dev.eliasnvx.femboymod.mixin.ServerGamePacketListenerAccessor) player.connection).femboymod$connection().isConnected();
    }

    /** Players without femboymod (vanilla clients on a modded server) have no receiver for our channels. */
    public static boolean canReceive(ServerPlayer player, ResourceLocation id) {
        return dev.eliasnvx.femboymod.platform.PlatformHelper.canReceive(player, id);
    }

    /** @return a fresh buffer holding the message body */
    public static FriendlyByteBuf encode(FemboyPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);
        return buf;
    }

    /** Architectury 9: C2S receivers are registered on both sides (the client side only announces the channel). */
    private static <T> void registerC2S(ResourceLocation id, Function<FriendlyByteBuf, T> reader,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        NetworkManager.registerReceiver(NetworkManager.c2s(), id, (buf, context) -> handler.accept(reader.apply(buf), context));
    }

    /**
     * Architectury 9: S2C receivers use client-only networking classes, so they are registered on the client only;
     * a dedicated server needs nothing to send on the channel. The handlers use common classes only.
     */
    private static <T> void registerS2C(ResourceLocation id, Function<FriendlyByteBuf, T> reader,
            BiConsumer<T, NetworkManager.PacketContext> handler) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), id, (buf, context) -> handler.accept(reader.apply(buf), context));
        }
    }
}
