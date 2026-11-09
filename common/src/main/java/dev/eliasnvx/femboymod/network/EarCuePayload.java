package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * S2C: a reason for a player's reactive ears and tail that only the server knows (mob effects of other players
 * never reach other clients). Sent to everyone tracking the player and to the player.
 */
public record EarCuePayload(int entityId, int cue, int ticks) implements CustomPacketPayload {

    public static final Type<EarCuePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "ear_cue"));
    public static final StreamCodec<ByteBuf, EarCuePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EarCuePayload::entityId, ByteBufCodecs.VAR_INT, EarCuePayload::cue,
            ByteBufCodecs.VAR_INT, EarCuePayload::ticks, EarCuePayload::new);

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
    public Type<EarCuePayload> type() {
        return TYPE;
    }

    public static void handle(EarCuePayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> onClient.accept(payload.entityId(), Cue.byId(payload.cue()), payload.ticks()));
    }

    /** Server: tells everyone who sees {@code player} (and the player) about the cue; 0 ticks stops it. */
    public static void send(Player player, Cue cue, int ticks) {
        if (!(player.level() instanceof ServerLevel level) || ticks < 0) {
            return;
        }
        EarCuePayload payload = new EarCuePayload(player.getId(), cue.ordinal(), ticks);
        for (ServerPlayer watcher : level.getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (NetworkManager.canPlayerReceive(watcher, TYPE)) {
                NetworkManager.sendToPlayer(watcher, payload);
            }
        }
    }

    /** Server: tells one player (who just started seeing {@code about}) about a cue that's still going. */
    public static void sendTo(ServerPlayer receiver, Player about, Cue cue, int ticks) {
        if (ticks > 0 && NetworkManager.canPlayerReceive(receiver, TYPE)) {
            NetworkManager.sendToPlayer(receiver, new EarCuePayload(about.getId(), cue.ordinal(), ticks));
        }
    }
}
