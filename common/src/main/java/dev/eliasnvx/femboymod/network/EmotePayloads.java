package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.emote.EmoteClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Emotes: the player asks to play one (C2S); the server shows it to everyone tracking them and to themselves (S2C). */
public final class EmotePayloads {

    private static final int HEART_COUNT = 5;

    private EmotePayloads() {
    }

    /** C2S: play this emote. */
    public record Play(int emote) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "play_emote"));
        public static final StreamCodec<ByteBuf, Play> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Play::new, Play::emote);

        @Override
        public Type<Play> type() {
            return TYPE;
        }

        public static void handle(Play payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player && !player.isSpectator() && player.isAlive()) {
                    play(player, Emote.byId(payload.emote()));
                }
            });
        }
    }

    /** S2C: an entity plays an emote. */
    public record Show(int entityId, int emote) implements CustomPacketPayload {
        public static final Type<Show> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "show_emote"));
        public static final StreamCodec<ByteBuf, Show> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Show::entityId, ByteBufCodecs.VAR_INT, Show::emote, Show::new);

        @Override
        public Type<Show> type() {
            return TYPE;
        }

        public static void handle(Show payload, NetworkManager.PacketContext context) {
            context.queue(() -> EmoteClientHooks.onShow.accept(payload.entityId(), Emote.byId(payload.emote())));
        }
    }

    /** Server: broadcast the emote; heart hands also send up a few hearts. */
    public static void play(ServerPlayer player, Emote emote) {
        Show show = new Show(player.getId(), emote.ordinal());
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (NetworkManager.canPlayerReceive(watcher, Show.TYPE)) {
                NetworkManager.sendToPlayer(watcher, show);
            }
        }
        if (emote == Emote.HEART_HANDS) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2.4, player.getZ(),
                    HEART_COUNT, 0.3, 0.2, 0.3, 0.0);
        }
    }
}
