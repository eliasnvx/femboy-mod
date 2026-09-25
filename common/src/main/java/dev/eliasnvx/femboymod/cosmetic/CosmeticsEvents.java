package dev.eliasnvx.femboymod.cosmetic;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/** Game hooks for cosmetic slots: sync, right-click equip, death drops. */
public final class CosmeticsEvents {

    private CosmeticsEvents() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(CosmeticsSyncPayload::sendToTrackingAndSelf);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> CosmeticsSyncPayload.sendToTrackingAndSelf(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> CosmeticsSyncPayload.sendToTrackingAndSelf(player));
        EntityEvent.START_TRACKING.register((entity, watcher) -> {
            if (entity instanceof Player tracked) {
                CosmeticsSyncPayload.sendTo(watcher, tracked);
            }
        });

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                dropOnDeath(player);
            }
            return EventResult.pass();
        });

        InteractionEvent.USE_ITEM.register((level, player, hand) -> equipFromHand(player, hand));
    }

    // TODO(Phase 4, SPEC §4.3/§10): honor the keepCosmeticsOnDeath config option in addition to keepInventory.
    public static void dropOnDeath(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return; // the attachment is copyOnDeath, so cosmetics carry over to the respawned player
        }
        for (ItemStack stack : CosmeticsManager.clear(player)) {
            player.spawnAtLocation(level, stack);
        }
    }

    /** Right-click a cosmetic in hand to wear it; swaps with whatever was in the slot. */
    public static EventResult equipFromHand(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        Identifier slot = CosmeticsManager.slotOf(held);
        if (slot == null || !CosmeticsManager.canEquip(player, slot, held)) {
            return EventResult.pass();
        }
        if (player instanceof ServerPlayer) {
            ItemStack worn = CosmeticsManager.get(player).get(slot).copy();
            ItemStack toWear = held.split(1);
            CosmeticsManager.set(player, slot, toWear);
            if (!worn.isEmpty()) {
                if (held.isEmpty()) {
                    player.setItemInHand(hand, worn);
                } else if (!player.getInventory().add(worn)) {
                    player.spawnAtLocation((ServerLevel) player.level(), worn);
                }
            }
        }
        return EventResult.fromMinecraft(InteractionResult.SUCCESS);
    }
}
