package dev.eliasnvx.femboymod.halloween;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Trick-or-treat: in the Halloween season ({@link HalloweenSeason}), a player in costume (anything in
 * {@code #femboymod:halloween_costume}) who sneaks and right-clicks an adult villager with an empty hand gets candy,
 * once per villager per game day (saved with the villager). Asked again the same day, the villager shakes its head.
 * Otherwise the click goes on to trading as usual.
 */
public final class TrickOrTreat {

    private static final long TICKS_PER_DAY = 24000L;
    private static final int HEAD_SHAKE_TICKS = 40;
    private static final int PARTICLES = 6;

    private TrickOrTreat() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(TrickOrTreat::interact);
    }

    static EventResult interact(Player player, Entity entity, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                || !(entity instanceof Villager villager) || villager.isBaby() || villager.isSleeping()
                || !HalloweenSeason.active() || !inCostume(player)) {
            return EventResult.pass();
        }
        if (player.level() instanceof ServerLevel level) {
            treat(level, player, villager);
        }
        return EventResult.interruptTrue();
    }

    /** Hands out candy if the villager hasn't today; returns how many were given (0 = shook its head). */
    public static int treat(ServerLevel level, Player player, Villager villager) {
        long day = level.getOverworldClockTime() / TICKS_PER_DAY;
        if (PlatformHelper.getLastTreatDay(villager) == day) {
            villager.setUnhappyCounter(HEAD_SHAKE_TICKS);
            villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, villager.getVoicePitch());
            return 0;
        }
        PlatformHelper.setLastTreatDay(villager, day);
        CommonConfig.Halloween config = FemboyConfig.common().halloween();
        int count = config.candyMin() + villager.getRandom().nextInt(Math.max(1, config.candyMax() - config.candyMin() + 1));
        if (count > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(FemboyItems.CANDY.get(), count), Prediction.SERVER_ONLY);
        }
        villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, villager.getVoicePitch());
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY(), villager.getZ(), PARTICLES, 0.4, 0.3, 0.4, 0.0);
        return count;
    }

    /** Wears at least one Halloween piece (in our slots or another mod's). */
    public static boolean inCostume(Player player) {
        CosmeticInventory worn = CosmeticsManager.worn(player);
        for (Identifier slot : CosmeticsManager.orderedSlots()) {
            if (worn.get(slot).is(FemboyTags.HALLOWEEN_COSTUME) && !worn.isHidden(slot)) {
                return true;
            }
        }
        return false;
    }
}
