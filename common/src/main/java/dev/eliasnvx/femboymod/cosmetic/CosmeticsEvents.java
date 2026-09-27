package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.eliasnvx.femboymod.network.ProfileSyncPayload;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import dev.eliasnvx.femboymod.registry.FemboyComponents;

/** Game hooks for cosmetic slots: sync, right-click equip, death drops. */
public final class CosmeticsEvents {

    private static boolean lastArmorRule = true;

    private CosmeticsEvents() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            CosmeticsSyncPayload.sendToTrackingAndSelf(player);
            ProfileSyncPayload.sendAll(player);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            CosmeticEffectsManager.forget(player);
            CosmeticsSyncPayload.sendToTrackingAndSelf(player);
            ProfileSyncPayload.sendAll(player);
        });
        PlayerEvent.PLAYER_QUIT.register(CosmeticEffectsManager::stop);
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                CosmeticEffectsManager.tick(serverPlayer);
                ProfileHooks.tick(serverPlayer);
            }
        });
        // allow_hidden_armor changed with /gamerule: resend everyone's outfit so clients redraw the armor
        TickEvent.SERVER_POST.register(dev.eliasnvx.femboymod.entity.CosplayerSpawner::tick);
        TickEvent.SERVER_POST.register(server -> {
            boolean allowed = server.overworld().getGameRules().get(FemboyGameRules.ALLOW_HIDDEN_ARMOR.get());
            if (allowed != lastArmorRule) {
                lastArmorRule = allowed;
                server.getPlayerList().getPlayers().forEach(CosmeticsSyncPayload::sendToTrackingAndSelf);
            }
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> CosmeticsSyncPayload.sendToTrackingAndSelf(player));
        EntityEvent.START_TRACKING.register((entity, watcher) -> {
            if (entity instanceof Player tracked) {
                CosmeticsSyncPayload.sendTo(watcher, tracked);
            }
        });

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                CosmeticEffectsManager.stop(player);
                dropOnDeath(player);
            }
            return EventResult.pass();
        });

        InteractionEvent.USE_ITEM.register((level, player, hand) -> equipFromHand(player, hand, false));
    }

    public static void dropOnDeath(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY) || level.getGameRules().get(FemboyGameRules.KEEP_COSMETICS.get())
                || FemboyConfig.common().keepCosmeticsOnDeath()) {
            return; // the attachment is copyOnDeath, so cosmetics carry over to the respawned player
        }
        for (ItemStack stack : CosmeticsManager.clear(player)) {
            player.spawnAtLocation(level, stack);
        }
    }

    /** Right-click a cosmetic in hand to wear it; swaps with whatever was in the slot. */
    public static EventResult equipFromHand(Player player, InteractionHand hand) {
        return equipFromHand(player, hand, false);
    }

    /**
     * @param force equip even items that have their own right-click action (backpacks call this on sneak-use)
     */
    public static EventResult equipFromHand(Player player, InteractionHand hand, boolean force) {
        ItemStack held = player.getItemInHand(hand);
        if (!force && held.has(FemboyComponents.BACKPACK.get())) {
            return EventResult.pass(); // backpacks open on right-click; BackpackItem handles sneak-equip
        }
        ResourceLocation slot = CosmeticsManager.slotOf(held);
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
