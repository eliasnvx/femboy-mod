package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.cosmetic.PvpCombat;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.world.FemboyGameRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S2C: cosmetic state of one player: in full to the player themselves, without packed contents to everyone else
 * ({@link CosmeticInventory#forOthers()}). Sent on join, respawn, dimension change and tracking start; changes are
 * queued with {@link #markDirty} and go out once at the end of the tick, however many there were.
 */
public record CosmeticsSyncPayload(int entityId, CosmeticInventory cosmetics, boolean armorHidingAllowed, boolean armorRevealed)
        implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "cosmetics_sync");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        writeInventory(buf, cosmetics);
        buf.writeBoolean(armorHidingAllowed);
        buf.writeBoolean(armorRevealed);
    }

    public static CosmeticsSyncPayload read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CosmeticInventory cosmetics = readInventory(buf);
        boolean armorHidingAllowed = buf.readBoolean();
        return new CosmeticsSyncPayload(entityId, cosmetics, armorHidingAllowed, buf.readBoolean());
    }

    /** Items per slot, hidden slots, then armor visibility per armor slot (as the 1.21.1 stream codec did). */
    private static void writeInventory(FriendlyByteBuf buf, CosmeticInventory inventory) {
        buf.writeMap(inventory.items(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeItem);
        buf.writeCollection(inventory.hidden(), FriendlyByteBuf::writeResourceLocation);
        buf.writeMap(inventory.armor(), FriendlyByteBuf::writeEnum, FriendlyByteBuf::writeEnum);
    }

    private static CosmeticInventory readInventory(FriendlyByteBuf buf) {
        Map<ResourceLocation, ItemStack> items = buf.readMap(HashMap::new, FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readItem);
        Set<ResourceLocation> hidden = buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation);
        Map<EquipmentSlot, ArmorVisibility> armor = buf.readMap(HashMap::new,
                b -> b.readEnum(EquipmentSlot.class), b -> b.readEnum(ArmorVisibility.class));
        return new CosmeticInventory(items, hidden, armor);
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

    /** Receiver (client side). Uses only common classes; registered on the client only (see FemboyNetwork). */
    public static void handle(CosmeticsSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> EarlySyncs.withPlayer(context, self -> {
            ArmorHiding.setAllowedOnClient(payload.armorHidingAllowed());
            Entity entity = self.getId() == payload.entityId() ? self : self.level().getEntity(payload.entityId());
            if (entity instanceof Player player) {
                PlatformHelper.setCosmetics(player, payload.cosmetics());
                ArmorHiding.setRevealed(player, payload.armorRevealed());
            }
        }));
    }

    /** Players without femboymod (vanilla clients on a modded server) cannot decode the payload. */
    public static boolean canReceive(ServerPlayer player) {
        return FemboyNetwork.canReceive(player, ID);
    }

    public static void sendTo(ServerPlayer receiver, Player about) {
        if (canReceive(receiver)) {
            FemboyNetwork.sendToPlayer(receiver, of(about, receiver == about));
        }
    }

    /** Sends now: the full state to the player, the visible state to everyone tracking them. */
    public static void sendToTrackingAndSelf(ServerPlayer player) {
        // 1.20.1 has no filtered tracking broadcast; players in view of the chunk (the self included) are a superset
        // of the trackers, and the receiver ignores entity ids it does not know.
        List<ServerPlayer> watchers = new ArrayList<>();
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && canReceive(watcher)) {
                watchers.add(watcher);
            }
        }
        if (!watchers.isEmpty()) {
            FemboyNetwork.sendToPlayers(watchers, of(player, false));
        }
        if (canReceive(player)) {
            FemboyNetwork.sendToPlayer(player, of(player, true));
        }
    }
}
