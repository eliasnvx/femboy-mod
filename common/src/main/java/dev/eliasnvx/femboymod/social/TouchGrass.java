package dev.eliasnvx.femboymod.social;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Touch grass (0.3.0). The server counts each player's play time since they joined or last touched grass; after
 * {@code plants.touch_grass_reminder_minutes} (0 = off) that player alone gets a gentle chat reminder, again every
 * interval. Right-clicking anything in {@code #femboymod:touchable_grass} (grass blocks, grass, Potted Grass) with an
 * empty hand resets the timer, rustles, and gives a short Regeneration plus the advancement (the buff at most once
 * per {@code plants.touch_grass_buff_cooldown_ticks}, so a lawn isn't a healing station).
 *
 * <p>Counts server ticks, so a paused singleplayer game doesn't count. The timer is per session (cleared on logout);
 * the buff cooldown runs on game time and outlives a relog, so rejoining doesn't refresh it (cleared when the server
 * stops: another world has another clock).
 */
public final class TouchGrass {

    private static final int TICKS_PER_MINUTE = 1200;
    /** Reminder checks run once a second. */
    private static final int CHECK_INTERVAL = 20;
    private static final int LEAVES = 5;
    private static final double LEAF_SPREAD = 0.3;
    /** Each online player's timer (server ticks) and when players last got the buff (game time). Server thread only. */
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_BUFF = new HashMap<>();

    /** Playing since {@code since} without touching grass; the next reminder is due at {@code due}. */
    public record Session(long since, long due) {
    }

    private TouchGrass() {
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(player -> resetTimer(player, player.level().getServer().getTickCount()));
        PlayerEvent.PLAYER_QUIT.register(TouchGrass::forget);
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            SESSIONS.clear();
            LAST_BUFF.clear();
        });
        TickEvent.SERVER_POST.register(TouchGrass::tick);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(TouchGrass::rightClick);
    }

    /** The reminder interval in ticks, or 0 when reminders are off. */
    public static long reminderTicks(CommonConfig.Plants config) {
        return (long) config.touchGrassReminderMinutes() * TICKS_PER_MINUTE;
    }

    /** Is the buff ready again ({@code lastBuff} null = never had it)? */
    public static boolean buffReady(Long lastBuff, long now, CommonConfig.Plants config) {
        return lastBuff == null || now - lastBuff >= config.touchGrassBuffCooldownTicks();
    }

    public static void resetTimer(ServerPlayer player, long now) {
        SESSIONS.put(player.getUUID(), new Session(now, now + reminderTicks(FemboyConfig.common().plants())));
    }

    /** The player's timer, or null if they aren't tracked. For GameTests. */
    public static Session session(ServerPlayer player) {
        return SESSIONS.get(player.getUUID());
    }

    /** Logout: the timer ends with the session (the buff cooldown stays, see the class docs). */
    public static void forget(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        long interval = reminderTicks(FemboyConfig.common().plants());
        if (now % CHECK_INTERVAL != 0 || interval <= 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Session session = SESSIONS.get(player.getUUID());
            if (session == null) {
                resetTimer(player, now); // joined before reminders were turned on
            } else if (now >= session.due()) {
                remind(player, session, now, interval);
            }
        }
    }

    /** Sends the reminder to this player only and starts the next interval. */
    public static void remind(ServerPlayer player, Session session, long now, long interval) {
        long minutes = (now - session.since()) / TICKS_PER_MINUTE;
        player.sendSystemMessage(Component.translatable("message.femboymod.touch_grass.reminder", minutes)
                .withStyle(ChatFormatting.GREEN));
        SESSIONS.put(player.getUUID(), new Session(session.since(), now + interval));
    }

    private static EventResult rightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || player.isSpectator()
                || !player.level().getBlockState(pos).is(FemboyTags.TOUCHABLE_GRASS)) {
            return EventResult.pass();
        }
        if (player instanceof ServerPlayer toucher && toucher.level() instanceof ServerLevel level) {
            touch(level, toucher, pos);
        }
        return EventResult.interruptTrue();
    }

    /** The player touched grass at {@code pos} (server). Returns whether they got the buff this time. */
    public static boolean touch(ServerLevel level, ServerPlayer player, BlockPos pos) {
        long now = level.getServer().getTickCount();
        resetTimer(player, now);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, LEAVES,
                LEAF_SPREAD, 0.1, LEAF_SPREAD, 0.0);
        level.playSound(null, pos, SoundEvents.GRASS_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        CommonConfig.Plants config = FemboyConfig.common().plants();
        long gameTime = level.getGameTime();
        if (!buffReady(LAST_BUFF.get(player.getUUID()), gameTime, config)) {
            return false;
        }
        LAST_BUFF.put(player.getUUID(), gameTime);
        if (config.touchGrassBuffTicks() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, config.touchGrassBuffTicks(), 0));
        }
        player.sendOverlayMessage(Component.translatable("message.femboymod.touch_grass.touched"));
        FemboyTriggers.fire(player, FemboyTriggers.TOUCH_GRASS);
        return true;
    }
}
