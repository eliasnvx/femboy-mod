package dev.eliasnvx.femboymod.entity;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Catnip and cats (0.3.0). A cat that smells catnip nearby (planted or potted) or gets fed a sprig goes through one
 * high: zoomies (a fast run around), mischief (it hunts for things to push off tables, {@link CatKnockGoal}), then it
 * lies down purring; afterwards more catnip does nothing for a while. The whole high is one hidden mob effect
 * ({@link FemboyEffects#CATNIP}) and the phase comes from its remaining time, so it survives saving and
 * {@code /effect clear} ends it. Feeding catnip to an untamed Stray Cat has a good chance to tame it. Numbers in config
 * {@code plants.catnip_*}; the goals come with the cat goals mixin.
 */
public final class Catnip {

    /** Where in the high a cat is; {@link #TOLERANT} cats ignore catnip. */
    public enum Phase {
        NONE, ZOOMIES, MISCHIEF, PURR, TOLERANT
    }

    private Catnip() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(Catnip::interact);
    }

    /** The whole high, tolerance included (ticks). */
    public static int duration(CommonConfig.Plants config) {
        return config.catnipZoomiesTicks() + config.catnipMischiefTicks() + config.catnipPurrTicks() + config.catnipToleranceTicks();
    }

    /** The phase for the effect's remaining ticks: the phases run in order, so the last one is closest to 0. */
    public static Phase phase(int remaining, CommonConfig.Plants config) {
        int tolerant = config.catnipToleranceTicks();
        int purr = tolerant + config.catnipPurrTicks();
        int mischief = purr + config.catnipMischiefTicks();
        if (remaining <= 0) {
            return Phase.NONE;
        } else if (remaining <= tolerant) {
            return Phase.TOLERANT;
        } else if (remaining <= purr) {
            return Phase.PURR;
        } else if (remaining <= mischief) {
            return Phase.MISCHIEF;
        }
        return Phase.ZOOMIES;
    }

    public static Phase phase(LivingEntity cat) {
        MobEffectInstance high = cat.getEffect(FemboyEffects.holder(FemboyEffects.CATNIP));
        return high == null ? Phase.NONE : phase(high.getDuration(), FemboyConfig.common().plants());
    }

    /** Starts the high, unless the cat already has catnip in its system. Server side. */
    public static boolean start(Cat cat) {
        if (phase(cat) != Phase.NONE) {
            return false;
        }
        cat.addEffect(new MobEffectInstance(FemboyEffects.holder(FemboyEffects.CATNIP), duration(FemboyConfig.common().plants()), 0,
                false, false, false));
        return true;
    }

    /** Is there catnip (anything in {@code #femboymod:catnip}) within {@code radius} blocks of {@code center}? */
    public static boolean smells(Level level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(pos).is(FemboyTags.CATNIP)) {
                return true;
            }
        }
        return false;
    }

    /** Right-click a cat with catnip: it eats the sprig and the high starts (or, already high, it isn't interested). */
    public static EventResult interact(Player player, Entity entity, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(entity instanceof Cat cat) || !stack.is(FemboyTags.CATNIP_ITEMS) || player.isSpectator()) {
            return EventResult.pass();
        }
        if (player instanceof ServerPlayer feeder && cat.level() instanceof ServerLevel level) {
            feed(level, cat, feeder, stack);
        }
        return EventResult.interruptTrue();
    }

    /** Feeds one sprig (server). Returns false if the cat wasn't interested (already high or still tolerant). */
    public static boolean feed(ServerLevel level, Cat cat, ServerPlayer feeder, ItemStack stack) {
        return feed(level, cat, feeder, stack, FemboyConfig.common().plants().strayCatTameChance());
    }

    /** {@link #feed(ServerLevel, Cat, ServerPlayer, ItemStack)} with its own taming chance (GameTests). */
    public static boolean feed(ServerLevel level, Cat cat, ServerPlayer feeder, ItemStack stack, float tameChance) {
        if (!start(cat)) {
            level.broadcastEntityEvent(cat, EntityEvent.TAMING_FAILED); // a little puff: not now
            return false;
        }
        stack.consume(1, feeder);
        cat.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F); // cats' own eat sound is per sound variant, protected
        if (cat.isOrderedToSit() && cat.isOwnedBy(feeder)) {
            cat.setOrderedToSit(false); // its owner's catnip is a good reason to get up
        }
        if (cat instanceof StrayCat && !cat.isTame()) {
            if (cat.getRandom().nextFloat() < tameChance) {
                cat.tame(feeder); // no sitting down: the zoomies come first
                cat.setPersistenceRequired();
                level.broadcastEntityEvent(cat, EntityEvent.TAMING_SUCCEEDED);
            } else {
                level.broadcastEntityEvent(cat, EntityEvent.TAMING_FAILED);
            }
        }
        FemboyTriggers.fire(feeder, FemboyTriggers.CATNIP);
        return true;
    }
}
