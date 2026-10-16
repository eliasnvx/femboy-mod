package dev.eliasnvx.femboymod.social;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.event.social.HeadpatEvent;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.entity.RubberDuck;
import dev.eliasnvx.femboymod.network.EmotePayloads;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Headpats: sneak and right-click another player or a tamed pet with an empty hand. The patter's hand pats (an emote
 * everyone sees), hearts float up, the target becomes Cozy for a while (slow healing) and a pet also gets a little
 * health back at once. Rubber ducks keep their own sneak-click (sit). Addons can change or cancel it
 * ({@link HeadpatEvent}).
 */
public final class Headpats {

    private static final int HEARTS = 4;
    private static final double HEART_SPREAD = 0.25;
    private static final float PAT_VOLUME = 0.8F;
    /** When the patter last gave a headpat (game time), so holding the button doesn't spam. Server thread only. */
    private static final Map<UUID, Long> LAST_PAT = new HashMap<>();

    private Headpats() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(Headpats::interact);
    }

    /** Sneaking, empty main hand, a pattable target; on the server, pats (when not on cooldown). */
    public static EventResult interact(Player player, Entity entity, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                || !(entity instanceof LivingEntity target) || !canPat(target) || player.isSpectator()) {
            return EventResult.pass();
        }
        if (player.level() instanceof ServerLevel level && player instanceof ServerPlayer patter) {
            pat(level, patter, target);
        }
        return EventResult.interruptTrue();
    }

    /** Other players (not spectators) and tamed pets, except rubber ducks (sneak-click makes them sit). */
    public static boolean canPat(LivingEntity target) {
        if (target instanceof Player other) {
            return !other.isSpectator() && other.isAlive();
        }
        return target instanceof TamableAnimal pet && pet.isTame() && pet.isAlive() && !(target instanceof RubberDuck);
    }

    /** Gives the headpat (server). Returns false if it was on cooldown or a listener cancelled it. */
    public static boolean pat(ServerLevel level, ServerPlayer patter, LivingEntity target) {
        CommonConfig.Friends config = FemboyConfig.common().friends();
        long now = level.getGameTime();
        Long last = LAST_PAT.get(patter.getUUID());
        if (last != null && now - last < config.headpatCooldownTicks()) {
            return false;
        }
        HeadpatEvent event = FemboyMod.api().events().post(new HeadpatEvent(patter, target, config.cozyTicks()));
        if (event.isCancelled()) {
            return false;
        }
        LAST_PAT.put(patter.getUUID(), now);
        if (event.cozyTicks() > 0) {
            target.addEffect(new MobEffectInstance(FemboyEffects.COZY.asHolder(), event.cozyTicks(), 0, false, false, true));
        }
        if (target instanceof TamableAnimal pet && pet.getHealth() < pet.getMaxHealth()) {
            pet.heal(config.petHeadpatHeal());
        }
        EmotePayloads.play(patter, Emote.HEADPAT);
        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY() + HEART_SPREAD, target.getZ(), HEARTS,
                HEART_SPREAD, HEART_SPREAD, HEART_SPREAD, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), FemboySounds.HEADPAT.get(), SoundSource.PLAYERS,
                PAT_VOLUME, 1.0F);
        FemboyTriggers.fire(patter, target instanceof Player ? FemboyTriggers.HEADPAT_PLAYER : FemboyTriggers.HEADPAT_PET);
        return true;
    }
}
