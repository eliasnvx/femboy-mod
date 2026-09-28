package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/** S2C: full cosmetic state of one player. Sent on change, on tracking start and on join/respawn. */
public record CosmeticsSyncPayload(int entityId, CosmeticInventory cosmetics, boolean armorHidingAllowed) implements FemboyPacket {

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
    }

    public static CosmeticsSyncPayload read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        CosmeticInventory cosmetics = readInventory(buf);
        return new CosmeticsSyncPayload(entityId, cosmetics, buf.readBoolean());
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

    public static CosmeticsSyncPayload of(Player player) {
        boolean allowed = !(player.level() instanceof ServerLevel level) || level.getGameRules().getBoolean(FemboyGameRules.ALLOW_HIDDEN_ARMOR);
        return new CosmeticsSyncPayload(player.getId(), CosmeticsManager.get(player), allowed);
    }

    /** Receiver (client side). Uses only common classes; registered on the client only (see FemboyNetwork). */
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
        return FemboyNetwork.canReceive(player, ID);
    }

    public static void sendTo(ServerPlayer receiver, Player about) {
        if (canReceive(receiver)) {
            FemboyNetwork.sendToPlayer(receiver, of(about));
        }
    }

    public static void sendToTrackingAndSelf(ServerPlayer player) {
        // 1.20.1 has no filtered tracking broadcast; players in view of the chunk (the self included) are a superset
        // of the trackers, and the receiver ignores entity ids it does not know.
        List<ServerPlayer> receivers = new ArrayList<>();
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && canReceive(watcher)) {
                receivers.add(watcher);
            }
        }
        if (canReceive(player)) {
            receivers.add(player);
        }
        if (!receivers.isEmpty()) {
            FemboyNetwork.sendToPlayers(receivers, of(player));
        }
    }
}
