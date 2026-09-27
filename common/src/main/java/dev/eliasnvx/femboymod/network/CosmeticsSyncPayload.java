package dev.eliasnvx.femboymod.network;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** S2C: full cosmetic state of one player. Sent on change, on tracking start and on join/respawn. */
public record CosmeticsSyncPayload(int entityId, CosmeticInventory cosmetics, boolean armorHidingAllowed) implements CustomPacketPayload {

    public static final Type<CosmeticsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosmetics_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CosmeticsSyncPayload::entityId,
            CosmeticInventory.STREAM_CODEC, CosmeticsSyncPayload::cosmetics,
            ByteBufCodecs.BOOL, CosmeticsSyncPayload::armorHidingAllowed,
            CosmeticsSyncPayload::new);

    @Override
    public Type<CosmeticsSyncPayload> type() {
        return TYPE;
    }

    public static CosmeticsSyncPayload of(Player player) {
        boolean allowed = !(player.level() instanceof ServerLevel level) || level.getGameRules().getBoolean(FemboyGameRules.ALLOW_HIDDEN_ARMOR);
        return new CosmeticsSyncPayload(player.getId(), CosmeticsManager.get(player), allowed);
    }

    /** Receiver (client side). Uses only common classes, so it is safe to register everywhere. */
    public static void handle(CosmeticsSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ArmorHiding.setAllowedOnClient(payload.armorHidingAllowed());
            Entity entity = context.getPlayer().level().getEntity(payload.entityId());
            if (entity instanceof Player player) {
                PlatformHelper.setCosmetics(player, payload.cosmetics());
            }
        });
    }

    /** Players without femboymod (vanilla clients on a modded server) cannot decode the payload. */
    public static boolean canReceive(ServerPlayer player) {
        return NetworkManager.canPlayerReceive(player, TYPE);
    }

    public static void sendTo(ServerPlayer receiver, Player about) {
        if (canReceive(receiver)) {
            NetworkManager.sendToPlayer(receiver, of(about));
        }
    }

    @SuppressWarnings("unchecked")
    public static void sendToTrackingAndSelf(ServerPlayer player) {
        Packet<? super ClientGamePacketListener> packet = (Packet<? super ClientGamePacketListener>)
                NetworkManager.toPacket(NetworkManager.s2c(), of(player), player.registryAccess());
        // 1.21.1 has no filtered tracking broadcast; players in view of the chunk (the self included) are a superset
        // of the trackers, and the receiver ignores entity ids it does not know.
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && canReceive(watcher)) {
                watcher.connection.send(packet);
            }
        }
        if (canReceive(player)) {
            player.connection.send(packet);
        }
    }
}
