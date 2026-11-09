package dev.eliasnvx.femboymod.cosmetic;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.profile.ProfileHooks;
import dev.eliasnvx.femboymod.network.ProfileSyncPayload;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import net.minecraft.world.entity.Entity;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.femboymod.effect.CosmeticEffectsManager;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import dev.eliasnvx.femboymod.registry.FemboyComponents;

/** Game hooks for cosmetic slots: sync, right-click equip, death drops (with the loader hooks). */
public final class CosmeticsEvents {

    private static boolean lastArmorRule = true;

    private CosmeticsEvents() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            CosmeticsSyncPayload.sendToTrackingAndSelf(player);
            ProfileSyncPayload.sendAll(player);
            ProfileHooks.onJoin(player);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) -> {
            CosmeticEffectsManager.forget(player);
            CosmeticsSyncPayload.sendToTrackingAndSelf(player);
            ProfileSyncPayload.sendAll(player);
        });
        PlayerEvent.PLAYER_QUIT.register(CosmeticEffectsManager::stop);
        PlayerEvent.PLAYER_QUIT.register(dev.eliasnvx.femboymod.photo.PhotoService::forget);
        PlayerEvent.PLAYER_QUIT.register(dev.eliasnvx.femboymod.network.EmotePayloads::forget);
        PlayerEvent.PLAYER_QUIT.register(dev.eliasnvx.femboymod.energy.EnergyDrinks::forgetEarCue);
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                CosmeticEffectsManager.tick(serverPlayer);
                ProfileHooks.tick(serverPlayer);
                dev.eliasnvx.femboymod.energy.EnergyDrinks.checkEarCue(serverPlayer);
            }
        });
        // allow_hidden_armor changed with /gamerule: resend everyone's outfit so clients redraw the armor
        TickEvent.SERVER_POST.register(dev.eliasnvx.femboymod.entity.CosplayerSpawner::tick);
        TickEvent.SERVER_POST.register(server -> {
            boolean allowed = server.overworld().getGameRules().getBoolean(FemboyGameRules.ALLOW_HIDDEN_ARMOR);
            if (allowed != lastArmorRule) {
                lastArmorRule = allowed;
                server.getPlayerList().getPlayers().forEach(CosmeticsSyncPayload::sendToTrackingAndSelf);
            }
        });
        // outfit changes queued during the tick go out once, at its end
        TickEvent.SERVER_POST.register(server -> CosmeticsSyncPayload.flushDirty());
        LifecycleEvent.SERVER_STOPPED.register(server -> CosmeticsSyncPayload.clearDirty());
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> {
            CosmeticsSyncPayload.sendToTrackingAndSelf(player);
            // NeoForge builds a new client player for the new dimension without copying attachments: the profile too
            ProfileSyncPayload.sendAll(player);
        });
        // Start tracking: Architectury 13 has no event for it, each loader calls onStartTracking

        // effects only (a death another mod cancels resumes them next tick); the slots drop with the vanilla death
        // loot, from the loader hook (PlatformHelperImpl): a cancelled death keeps them, grave mods can collect them
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                CosmeticEffectsManager.stop(player);
            }
            return EventResult.pass();
        });

        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> equipFromHand(player, hand, false).interruptsFurtherEvaluation()
                ? CompoundEventResult.interruptTrue(player.getItemInHand(hand))
                : CompoundEventResult.pass());
    }

    /**
     * {@code watcher} starts seeing {@code entity}: send the outfit (and a caffeine ear twitch still going) if it is a
     * player. Called by the loader entrypoints.
     */
    public static void onStartTracking(Entity entity, ServerPlayer watcher) {
        if (entity instanceof Player tracked) {
            // Fabric fires start tracking before the spawn packet goes out: sent now, the outfit reaches a client that
            // doesn't know the player yet and is dropped (1.20.1 Fabric, a player walking back into view showed
            // nothing). The server's next task runs after this tick sent the spawn packets.
            watcher.server.tell(new TickTask(watcher.server.getTickCount(), () -> {
                if (!watcher.hasDisconnected() && !tracked.isRemoved()) {
                    CosmeticsSyncPayload.sendTo(watcher, tracked);
                    dev.eliasnvx.femboymod.energy.EnergyDrinks.resendEarCue(watcher, tracked);
                }
            }));
        }
    }

    /**
     * Empties a dying player's slots and returns what they wore, or nothing when a keep rule applies (the attachment is
     * copyOnDeath, so kept cosmetics carry over to the respawned player). Called while the vanilla death loot drops:
     * NeoForge puts them into {@code LivingDropsEvent}'s drops (where grave mods look), Fabric spawns them right after.
     */
    public static List<ItemStack> takeDeathDrops(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || level.getGameRules().getBoolean(FemboyGameRules.KEEP_COSMETICS)
                || FemboyConfig.common().keepCosmeticsOnDeath()) {
            return List.of();
        }
        return CosmeticsManager.clear(player);
    }

    /** Drops a dying player's cosmetics at their feet (unless a keep rule applies). */
    public static void dropOnDeath(ServerPlayer player) {
        for (ItemStack stack : takeDeathDrops(player)) {
            player.spawnAtLocation(stack);
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
                    player.spawnAtLocation(worn);
                }
            }
        }
        return EventResult.interruptTrue(); // SUCCESS
    }
}
