package dev.eliasnvx.femboymod.network;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.cosmetic.PvpCombat;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * S2C: cosmetic state of one player: in full to the player themselves, without packed contents to everyone else
 * ({@link CosmeticInventory#forOthers()}). Sent on join, respawn, dimension change and tracking start; changes are
 * queued with {@link #markDirty} and go out once at the end of the tick, however many there were.
 */
public record CosmeticsSyncPayload(int entityId, CosmeticInventory cosmetics, boolean armorHidingAllowed, boolean armorRevealed)
        implements CustomPacketPayload {

    public static final Type<CosmeticsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosmetics_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CosmeticsSyncPayload::entityId,
            CosmeticInventory.STREAM_CODEC, CosmeticsSyncPayload::cosmetics,
            ByteBufCodecs.BOOL, CosmeticsSyncPayload::armorHidingAllowed,
            ByteBufCodecs.BOOL, CosmeticsSyncPayload::armorRevealed,
            CosmeticsSyncPayload::new);

    @Override
    public Type<CosmeticsSyncPayload> type() {
        return TYPE;
    }

    /** Players whose outfit changed this tick (identity: a respawn makes a new player object). Server thread only. */
    private static final Set<ServerPlayer> DIRTY = new LinkedHashSet<>();

    /** The full state, as the player themselves gets it. */
    public static CosmeticsSyncPayload of(Player player) {
        return of(player, true);
    }

    /** The state about {@code player}; {@code forOwner} false leaves out what's packed inside worn items. */
    public static CosmeticsSyncPayload of(Player player, boolean forOwner) {
        boolean allowed = !(player.level() instanceof ServerLevel level) || level.getGameRules().getBoolean(FemboyGameRules.ALLOW_HIDDEN_ARMOR);
        CosmeticInventory cosmetics = CosmeticsManager.get(player);
        return new CosmeticsSyncPayload(player.getId(), forOwner ? cosmetics : cosmetics.forOthers(), allowed,
                PvpCombat.revealsArmor(player));
    }

    /**
     * Queues a sync for the end of this tick: any number of changes in one tick go out as one packet, so spamming
     * toggles from a client can't multiply what everyone around receives.
     */
    public static void markDirty(ServerPlayer player) {
        DIRTY.add(player);
    }

    public static boolean isDirty(ServerPlayer player) {
        return DIRTY.contains(player);
    }

    /** End of the server tick: syncs every queued player that's still online. Returns how many were sent. */
    public static int flushDirty() {
        if (DIRTY.isEmpty()) {
            return 0;
        }
        List<ServerPlayer> queued = List.copyOf(DIRTY);
        DIRTY.clear();
        int sent = 0;
        for (ServerPlayer player : queued) {
            if (!player.isRemoved() && !player.hasDisconnected()) {
                sendToTrackingAndSelf(player);
                sent++;
            }
        }
        return sent;
    }

    public static void clearDirty() {
        DIRTY.clear();
    }

    /** Receiver (client side). Uses only common classes, so it is safe to register everywhere. */
    public static void handle(CosmeticsSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() == null) {
                return; // queued as the client left the world
            }
            ArmorHiding.setAllowedOnClient(payload.armorHidingAllowed());
            Entity entity = context.getPlayer().level().getEntity(payload.entityId());
            if (entity instanceof Player player) {
                PlatformHelper.setCosmetics(player, payload.cosmetics());
                ArmorHiding.setRevealed(player, payload.armorRevealed());
            }
        });
    }

    /** Players without femboymod (vanilla clients on a modded server) cannot decode the payload. */
    public static boolean canReceive(ServerPlayer player) {
        return NetworkManager.canPlayerReceive(player, TYPE);
    }

    public static void sendTo(ServerPlayer receiver, Player about) {
        if (canReceive(receiver)) {
            NetworkManager.sendToPlayer(receiver, of(about, receiver == about));
        }
    }

    /** Sends now: the full state to the player, the visible state to everyone tracking them. */
    @SuppressWarnings("unchecked")
    public static void sendToTrackingAndSelf(ServerPlayer player) {
        Packet<? super ClientGamePacketListener> visible = (Packet<? super ClientGamePacketListener>)
                NetworkManager.toPacket(NetworkManager.s2c(), of(player, false), player.registryAccess());
        // 1.21.1 has no filtered tracking broadcast; players in view of the chunk (the self included) are a superset
        // of the trackers, and the receiver ignores entity ids it does not know.
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && canReceive(watcher)) {
                watcher.connection.send(visible);
            }
        }
        if (canReceive(player)) {
            NetworkManager.sendToPlayer(player, of(player, true));
        }
    }
}
