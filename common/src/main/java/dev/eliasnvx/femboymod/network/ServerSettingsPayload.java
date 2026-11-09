package dev.eliasnvx.femboymod.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.ServerSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * S2C: the server's {@link ServerSettings}. Sent on join; a server tick notices changes (game rule, config reload)
 * and sends them to everyone.
 */
public record ServerSettingsPayload(ServerSettings settings) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "server_settings");

    /** How often the server looks for changed settings (ticks). */
    private static final int CHECK_INTERVAL = 20;
    /** What everyone online was last sent. Server thread only. */
    private static @Nullable ServerSettings lastSent;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        settings.write(buf);
    }

    public static ServerSettingsPayload read(FriendlyByteBuf buf) {
        return new ServerSettingsPayload(ServerSettings.read(buf));
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(player -> send(player, ServerSettings.of(player.server)));
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
        if (FemboyNetwork.canReceive(player, ID)) {
            FemboyNetwork.sendToPlayer(player, new ServerSettingsPayload(settings));
        }
    }

    /** Receiver (client side). Needs no player: the settings belong to the connection. */
    public static void handle(ServerSettingsPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> ServerSettings.setClient(payload.settings()));
    }
}
