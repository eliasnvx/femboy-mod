package dev.eliasnvx.femboymod.combat;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.combat.DripDamage;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.DamageBonusEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drip in combat (server side): {@link DripDamage} rules scale what mobs deal to stylish players, and active
 * {@code femboymod:damage_bonus} effects scale what a player deals to the listed mobs.
 * Called from {@code LivingEntityHurtMixin} before armor and effects are applied.
 */
public final class DripCombat {

    /** Player -> effect source id -> active bonus (added/removed by the effect's activate/deactivate). */
    private static final Map<UUID, Map<ResourceLocation, ActiveBonus>> BONUSES = new HashMap<>();

    private record ActiveBonus(DamageBonusEffect effect, double scale) {
    }

    private DripCombat() {
    }

    public static float modifyIncoming(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == victim) {
            return amount;
        }
        float result = amount;
        if (victim instanceof ServerPlayer player && !(attacker instanceof Player)) {
            int tier = FemboyMod.api().getDripLevel(player).tier();
            for (DripDamage rule : level.registryAccess().lookupOrThrow(DripDamage.REGISTRY_KEY)) {
                if (rule.attackers().contains(attacker.getType().builtInRegistryHolder())) {
                    result *= rule.multiplierFor(tier);
                }
            }
        }
        int pvpPercent = level.getGameRules().get(FemboyGameRules.DRIP_PVP_PERCENT.get());
        if (pvpPercent > 0 && attacker instanceof ServerPlayer attackerPlayer && victim instanceof ServerPlayer victimPlayer) {
            result *= pvpMultiplier(FemboyMod.api().getDripLevel(attackerPlayer).tier(), FemboyMod.api().getDripLevel(victimPlayer).tier(), pvpPercent);
        }
        if (attacker instanceof ServerPlayer player) {
            Map<ResourceLocation, ActiveBonus> bonuses = BONUSES.get(player.getUUID());
            if (bonuses != null) {
                for (ActiveBonus bonus : bonuses.values()) {
                    if (bonus.effect().targets().contains(victim.getType().builtInRegistryHolder())) {
                        result *= (float) (1.0 + (bonus.effect().multiplier() - 1.0) * bonus.scale());
                    }
                }
            }
        }
        return result;
    }

    /** Drip PvP: {@code percent}% more damage per tier the attacker is above the victim, less when below (never below 0). */
    public static float pvpMultiplier(int attackerTier, int victimTier, int percent) {
        return Math.max(0.0F, 1.0F + (attackerTier - victimTier) * percent / 100.0F);
    }

    public static void addBonus(ServerPlayer player, ResourceLocation source, DamageBonusEffect effect, double scale) {
        BONUSES.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>()).put(source, new ActiveBonus(effect, scale));
    }

    public static void removeBonus(ServerPlayer player, ResourceLocation source) {
        Map<ResourceLocation, ActiveBonus> bonuses = BONUSES.get(player.getUUID());
        if (bonuses != null) {
            bonuses.remove(source);
            if (bonuses.isEmpty()) {
                BONUSES.remove(player.getUUID());
            }
        }
    }

    /** Death/respawn: the new player entity starts without bonuses. */
    public static void clear(ServerPlayer player) {
        BONUSES.remove(player.getUUID());
    }
}
