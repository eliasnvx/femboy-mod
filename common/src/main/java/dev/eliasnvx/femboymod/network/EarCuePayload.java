package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: a reason for a player's reactive ears and tail that only the server knows (mob effects of other players
 * never reach other clients). Sent to everyone tracking the player and to the player.
 */
public record EarCuePayload(int entityId, int cue, int ticks) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "ear_cue");

    /** Client side handler (entity id, cue, ticks); set by the client entry point. */
    public static Handler onClient = (entityId, cue, ticks) -> {
    };

    public enum Cue {
        /** Got a headpat: the tail wags. */
        HEADPAT,
        /** Drank Byte Energy: ears and tail twitch while caffeinated. */
        CAFFEINE;

        static Cue byId(int id) {
            Cue[] values = values();
            return id >= 0 && id < values.length ? values[id] : HEADPAT;
        }
    }

    @FunctionalInterface
    public interface Handler {
        void accept(int entityId, Cue cue, int ticks);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(cue);
        buf.writeVarInt(ticks);
    }

    public static EarCuePayload read(FriendlyByteBuf buf) {
        return new EarCuePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(EarCuePayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> onClient.accept(payload.entityId(), Cue.byId(payload.cue()), payload.ticks()));
    }

    /** Server: tells everyone who sees {@code player} (and the player) about the cue; 0 ticks stops it. */
    public static void send(Player player, Cue cue, int ticks) {
        if (!(player.level() instanceof ServerLevel level) || ticks < 0) {
            return;
        }
        List<ServerPlayer> receivers = new ArrayList<>();
        for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (FemboyNetwork.canReceive(watcher, ID)) {
                receivers.add(watcher);
            }
        }
        if (!receivers.isEmpty()) {
            FemboyNetwork.sendToPlayers(receivers, new EarCuePayload(player.getId(), cue.ordinal(), ticks));
        }
    }

    /** Server: tells one player (who just started seeing {@code about}) about a cue that's still going. */
    public static void sendTo(ServerPlayer receiver, Player about, Cue cue, int ticks) {
        if (ticks > 0 && FemboyNetwork.canReceive(receiver, ID)) {
            FemboyNetwork.sendToPlayer(receiver, new EarCuePayload(about.getId(), cue.ordinal(), ticks));
        }
    }
}
