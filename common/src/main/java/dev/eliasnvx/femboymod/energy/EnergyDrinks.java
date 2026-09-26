package dev.eliasnvx.femboymod.energy;

import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.event.profile.EnergyDrinkEvent;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server logic of Byte Energy: buffs, crash, jitter. All numbers come from data packs. */
public final class EnergyDrinks {

    private EnergyDrinks() {
    }

    public static void onDrink(ServerLevel level, LivingEntity drinker, ItemStack can) {
        EnergyDrink drink = level.registryAccess().lookup(EnergyDrink.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(EnergyDrink.keyOf(can.getItem()))).orElse(null);
        if (drink == null) {
            return;
        }
        drink.effects().forEach(buff -> drinker.addEffect(buff.instance()));
        int duration = drink.longestDuration();
        MobEffectInstance current = drinker.getEffect(FemboyEffects.CAFFEINATED.asHolder());
        if (current == null || current.getDuration() < duration) {
            drinker.addEffect(new MobEffectInstance(FemboyEffects.CAFFEINATED.asHolder(), duration, 0, false, false, true));
        }

        if (drinker instanceof Player player) {
            CaffeineRules rules = rules(level);
            CaffeineLog log = PlatformHelper.getCaffeineLog(player).drink(level.getGameTime(), rules.jitterWindow());
            PlatformHelper.setCaffeineLog(player, log);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                dev.eliasnvx.femboymod.entity.FemboyTriggers.fire(serverPlayer, dev.eliasnvx.femboymod.entity.FemboyTriggers.ENERGY_DRINKS, log.total());
                FemboyMod.api().getProfile(serverPlayer).update(FemboyProfileFields.ENERGY_DRINKS, count -> count + 1);
                if (CosmeticsManager.get(serverPlayer).get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get())) {
                    dev.eliasnvx.femboymod.entity.FemboyTriggers.fire(serverPlayer, dev.eliasnvx.femboymod.entity.FemboyTriggers.READY_TO_DEPLOY);
                }
            }
            if (log.count() > rules.jitterAfter()) {
                rules.jitter().forEach(buff -> player.addEffect(buff.instance()));
            }
            FemboyMod.api().events().post(new EnergyDrinkEvent(player, can, log.count()));
        }
    }

    static void crash(ServerLevel level, LivingEntity entity) {
        rules(level).crash().forEach(buff -> entity.addEffect(buff.instance()));
    }

    private static CaffeineRules rules(ServerLevel level) {
        return level.registryAccess().lookup(CaffeineRules.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(CaffeineRules.DEFAULT)).orElse(CaffeineRules.FALLBACK);
    }
}
