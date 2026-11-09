package dev.eliasnvx.femboymod.cosmetic;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.network.CosmeticsSyncPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Players fighting other players show the armor they hid under their outfit: a player in netherite shouldn't look
 * unarmored to the one about to hit them. Hitting or getting hit by another player (projectiles count, by their
 * owner) reveals both for {@code pvp.reveal_hidden_armor_ticks}; the flag goes out with the cosmetics sync, so
 * everyone around sees the same. Server side; cleared on logout.
 */
public final class PvpCombat {

    /** How often expired fights are checked (ticks). */
    private static final int EXPIRY_CHECK = 20;
    /** Game time each fighting player's reveal ends. Server thread only. */
    private static final Map<UUID, Long> REVEALED_UNTIL = new HashMap<>();

    private PvpCombat() {
    }

    public static void init() {
        EntityEvent.LIVING_HURT.register(PvpCombat::onHurt);
        TickEvent.SERVER_POST.register(PvpCombat::tick);
        PlayerEvent.PLAYER_QUIT.register(player -> REVEALED_UNTIL.remove(player.getUUID()));
        LifecycleEvent.SERVER_STOPPED.register(server -> REVEALED_UNTIL.clear());
    }

    private static EventResult onHurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim instanceof ServerPlayer hurt && source.getEntity() instanceof ServerPlayer attacker && attacker != hurt) {
            fight(hurt, attacker);
        }
        return EventResult.pass();
    }

    /** Two players are fighting: both show their armor for a while. */
    public static void fight(ServerPlayer first, ServerPlayer second) {
        int ticks = FemboyConfig.common().pvp().revealHiddenArmorTicks();
        if (ticks <= 0) {
            return;
        }
        long until = first.level().getServer().overworld().getGameTime() + ticks;
        reveal(first, until);
        reveal(second, until);
    }

    private static void reveal(ServerPlayer player, long until) {
        Long before = REVEALED_UNTIL.put(player.getUUID(), until);
        if (before == null) {
            CosmeticsSyncPayload.markDirty(player); // starts showing now
        }
    }

    /** Whether this player's hidden armor is shown because they're fighting another player (server). */
    public static boolean revealsArmor(Player player) {
        return REVEALED_UNTIL.containsKey(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        if (REVEALED_UNTIL.isEmpty() || server.getTickCount() % EXPIRY_CHECK != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = REVEALED_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now >= entry.getValue()) {
                it.remove();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    CosmeticsSyncPayload.markDirty(player); // hidden again
                }
            }
        }
    }
}
