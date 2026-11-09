package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.emote.EmoteClientHooks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Emotes: the player asks to play one (C2S); the server shows it to everyone tracking them and to themselves (S2C). */
public final class EmotePayloads {

    /** When each player's current wheel emote ends (game time). Server thread only; cleared on logout. */
    private static final Map<UUID, Long> PLAYING_UNTIL = new HashMap<>();

    private static final int HEART_COUNT = 5;

    private EmotePayloads() {
    }

    /** C2S: play this emote. */
    public record Play(int emote) implements FemboyPacket {
        public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "play_emote");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(emote);
        }

        public static Play read(FriendlyByteBuf buf) {
            return new Play(buf.readVarInt());
        }

        public static void handle(Play payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                Emote emote = Emote.byId(payload.emote());
                if (context.getPlayer() instanceof ServerPlayer player && !player.isSpectator() && player.isAlive() && emote.inWheel()) {
                    tryPlay(player, emote);
                }
            });
        }
    }

    /** S2C: an entity plays an emote. */
    public record Show(int entityId, int emote) implements FemboyPacket {
        public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "show_emote");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeVarInt(emote);
        }

        public static Show read(FriendlyByteBuf buf) {
            return new Show(buf.readVarInt(), buf.readVarInt());
        }

        public static void handle(Show payload, NetworkManager.PacketContext context) {
            context.queue(() -> EmoteClientHooks.onShow.accept(payload.entityId(), Emote.byId(payload.emote())));
        }
    }

    /**
     * Server, from the wheel: plays the emote unless the player's last one is still playing. Every emote goes out to
     * everyone around (heart hands with particles too), so a client spamming the packet is held to one at a time.
     */
    public static boolean tryPlay(ServerPlayer player, Emote emote) {
        long now = player.level().getGameTime();
        Long until = PLAYING_UNTIL.get(player.getUUID());
        if (until != null && now < until) {
            return false;
        }
        PLAYING_UNTIL.put(player.getUUID(), now + emote.durationTicks());
        play(player, emote);
        return true;
    }

    public static void forget(ServerPlayer player) {
        PLAYING_UNTIL.remove(player.getUUID());
    }

    /** Server: broadcast the emote; heart hands also send up a few hearts. */
    public static void play(ServerPlayer player, Emote emote) {
        List<ServerPlayer> receivers = new ArrayList<>();
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (FemboyNetwork.canReceive(watcher, Show.ID)) {
                receivers.add(watcher);
            }
        }
        if (!receivers.isEmpty()) {
            FemboyNetwork.sendToPlayers(receivers, new Show(player.getId(), emote.ordinal()));
        }
        if (emote == Emote.HEART_HANDS) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2.4, player.getZ(),
                    HEART_COUNT, 0.3, 0.2, 0.3, 0.0);
        }
    }
}
