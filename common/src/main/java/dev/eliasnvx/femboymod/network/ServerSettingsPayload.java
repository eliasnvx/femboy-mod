package dev.eliasnvx.femboymod.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.ServerSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * S2C: the server's {@link ServerSettings}. Sent on join; a server tick notices changes (game rule, config reload)
 * and sends them to everyone.
 */
public record ServerSettingsPayload(ServerSettings settings) implements CustomPacketPayload {

    public static final Type<ServerSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "server_settings"));
    public static final StreamCodec<ByteBuf, ServerSettingsPayload> STREAM_CODEC =
            ServerSettings.STREAM_CODEC.map(ServerSettingsPayload::new, ServerSettingsPayload::settings);

    /** How often the server looks for changed settings (ticks). */
    private static final int CHECK_INTERVAL = 20;
    /** What everyone online was last sent. Server thread only. */
    private static @Nullable ServerSettings lastSent;

    @Override
    public Type<ServerSettingsPayload> type() {
        return TYPE;
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(player -> send(player, ServerSettings.of(player.level().getServer())));
        TickEvent.SERVER_POST.register(ServerSettingsPayload::tick);
        LifecycleEvent.SERVER_STOPPED.register(server -> lastSent = null);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        ServerSettings now = ServerSettings.of(server);
        if (!now.equals(lastSent)) {
            lastSent = now;
            server.getPlayerList().getPlayers().forEach(player -> send(player, now));
        }
    }

    private static void send(ServerPlayer player, ServerSettings settings) {
        if (NetworkManager.canPlayerReceive(player, TYPE)) {
            NetworkManager.sendToPlayer(player, new ServerSettingsPayload(settings));
        }
    }

    public static void handle(ServerSettingsPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> ServerSettings.setClient(payload.settings()));
    }
}
