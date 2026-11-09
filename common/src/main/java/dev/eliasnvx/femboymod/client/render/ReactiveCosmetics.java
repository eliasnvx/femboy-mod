package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticReactionSource;
import dev.eliasnvx.femboymod.api.client.CosmeticReactions;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.entity.SockGoblin;
import dev.eliasnvx.femboymod.network.EarCuePayload;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactive ears and tails (client side, visual only). Every few ticks, for each nearby player whose worn cosmetics
 * have a {@link EarHearing}, collects what should happen now (built-in reactions plus addon
 * {@link CosmeticReactionSource}s) and hands the targets to the player's {@link CosmeticRenderData}, which smooths
 * them per frame. Only uses what the client knows; the server-only cues (headpats, Byte Energy) arrive as
 * {@link EarCuePayload}.
 */
public final class ReactiveCosmetics {

    /** Each wearer is re-evaluated every this many ticks (staggered by entity id); smoothing hides the steps. */
    private static final int INTERVAL = 2;
    /** Wearers further than this from the local player are not evaluated: their ears are a few pixels big. */
    private static final double ACTIVE_RANGE = 48.0;
    /** Alertness for a mob at the edge of hearing (1 up close). */
    private static final float FAR_NOTICE = 0.55F;
    /** A mob from {@code notices_first} beats any other mob, however close. */
    private static final float FIRST_NOTICE_BONUS = 1.0F;
    private static final double PET_RANGE = 8.0;
    private static final float PET_WAG = 0.45F;
    private static final float LOW_HEALTH_START = 0.4F;
    private static final float LOW_HEALTH_FULL = 0.15F;
    private static final float UNDERWATER_FOLD = 0.7F;
    private static final float HURT_FLATTEN = 0.8F;
    private static final float CAFFEINE_JITTER = 0.6F;
    /** Expired cues are dropped this often. */
    private static final int CUE_CLEANUP_TICKS = 200;
    /**
     * Vanilla's sky darkness ({@code Level#updateSkyBrightness}): rain and thunder each dim the sky by up to 5/16, the
     * sun term swings between 0 and 1 over the day, and from a darkness of 4 of 11 on it counts as dark
     * ({@code Level#isNight}).
     */
    private static final float WEATHER_DIM = 5.0F / 16.0F;
    private static final double SUN_BASE = 0.5;
    private static final double SUN_SWING = 2.0;
    private static final double SUN_CLAMP = 0.25;
    private static final double DARKNESS_STEPS = 11.0;
    private static final int DARK_FROM = 4;

    private static final Collector COLLECTOR = new Collector();
    /** Entity id → game time until which the cue lasts. */
    private static final Int2LongOpenHashMap WAG_UNTIL = new Int2LongOpenHashMap();
    private static final Int2LongOpenHashMap JITTER_UNTIL = new Int2LongOpenHashMap();
    private static @Nullable List<CosmeticReactionSource> sources;
    private static boolean[] sourceFailed = new boolean[0];

    private ReactiveCosmetics() {
    }

    /** A cue from the server (the payload handler runs on the client thread). */
    public static void onCue(int entityId, EarCuePayload.Cue cue, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long until = level.getGameTime() + ticks;
        switch (cue) {
            case HEADPAT -> WAG_UNTIL.put(entityId, until);
            case CAFFEINE -> JITTER_UNTIL.put(entityId, until);
        }
    }

    /** Leaving the world: entity ids mean nothing in the next one. */
    public static void clear() {
        WAG_UNTIL.clear();
        JITTER_UNTIL.clear();
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        Player local = minecraft.player;
        if (level == null || local == null || !FemboyConfig.client().reactiveCosmetics()) {
            return;
        }
        List<CosmeticReactionSource> addonSources = sources();
        long time = level.getGameTime();
        boolean showOthers = FemboyConfig.client().showOthersCosmetics();
        boolean dark = isDark(level);
        for (Player player : level.players()) {
            if ((player.getId() + time) % INTERVAL != 0) {
                continue;
            }
            CosmeticRenderData data = CosmeticRenderData.of(player);
            if (data == null) {
                continue;
            }
            EarHearing hearing = data.hearing();
            if (hearing == null || player.isInvisible() || (player != local && !showOthers)
                    || player.distanceToSqr(local) > ACTIVE_RANGE * ACTIVE_RANGE) {
                data.setReactionTargets(0, Float.NaN, 0, 0, 0, 0, 0);
                continue;
            }
            COLLECTOR.reset();
            collectBuiltin(level, player, hearing, time, dark);
            for (int i = 0; i < addonSources.size(); i++) {
                try {
                    addonSources.get(i).collect(player, COLLECTOR);
                } catch (RuntimeException e) {
                    if (!sourceFailed[i]) { // log once, keep the others running
                        sourceFailed[i] = true;
                        FemboyMod.LOGGER.error("Cosmetic reaction source failed", e);
                    }
                }
            }
            COLLECTOR.applyTo(data, player);
        }
        if (time % CUE_CLEANUP_TICKS == 0) {
            WAG_UNTIL.int2LongEntrySet().removeIf(e -> e.getLongValue() < time);
            JITTER_UNTIL.int2LongEntrySet().removeIf(e -> e.getLongValue() < time);
        }
    }

    /**
     * Whether it's dark outside, worked out like vanilla's {@code Level#isNight}. A 1.20.1 client level computes its
     * sky darkness only once, when it is created (the server level every tick), so {@code isNight()} goes stale there.
     */
    private static boolean isDark(ClientLevel level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        double rain = 1.0 - level.getRainLevel(1.0F) * WEATHER_DIM;
        double thunder = 1.0 - level.getThunderLevel(1.0F) * WEATHER_DIM;
        double sun = SUN_BASE + SUN_SWING * Mth.clamp(Mth.cos(level.getTimeOfDay(1.0F) * Mth.TWO_PI), -SUN_CLAMP, SUN_CLAMP);
        return (int) ((1.0 - sun * rain * thunder) * DARKNESS_STEPS) >= DARK_FROM;
    }

    private static void collectBuiltin(ClientLevel level, Player player, EarHearing hearing, long time, boolean dark) {
        float radius = hearing.radius(dark);
        float firstRadius = hearing.firstRadius(dark);
        Entity best = null;
        float bestScore = 0;
        float bestStrength = 0;
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(firstRadius),
                e -> e instanceof Mob && e.isAlive())) {
            double distance = Math.sqrt(player.distanceToSqr(entity));
            if (entity instanceof Creeper creeper && distance <= radius) {
                float swell = creeper.getSwelling(1.0F);
                COLLECTOR.flatten(swell * hearing.flatten());
                COLLECTOR.puff(swell);
            }
            if (entity instanceof TamableAnimal pet && pet.isTame() && distance <= PET_RANGE && pet.isOwnedBy(player)) {
                COLLECTOR.wag(PET_WAG);
                continue;
            }
            boolean first = distance <= firstRadius
                    && (hearing.noticesFirst(entity) || entity instanceof SockGoblin goblin && goblin.hasLoot());
            boolean heard = first || distance <= radius && (entity instanceof Enemy || hearing.startles());
            if (!heard) {
                continue;
            }
            float strength = first ? 1.0F : ReactionMath.noticeStrength(distance, radius, FAR_NOTICE);
            float score = strength + (first ? FIRST_NOTICE_BONUS : 0.0F);
            if (score > bestScore) {
                best = entity;
                bestScore = score;
                bestStrength = strength;
            }
        }
        if (best != null) {
            COLLECTOR.notice(best, bestStrength);
        }

        if (player.hurtTime > 0 && player.hurtDuration > 0) {
            float hurt = player.hurtTime / (float) player.hurtDuration;
            COLLECTOR.flatten(hurt * HURT_FLATTEN * hearing.flatten());
            COLLECTOR.puff(hurt);
        }
        if (player.isSleeping() || player.isDeadOrDying()) {
            COLLECTOR.droop(1.0F);
        } else {
            COLLECTOR.droop(ReactionMath.lowHealthDroop(player.getHealth(), player.getMaxHealth(), LOW_HEALTH_START, LOW_HEALTH_FULL));
        }
        if (player.isUnderWater()) {
            COLLECTOR.flatten(UNDERWATER_FOLD * hearing.flatten());
        }
        if (WAG_UNTIL.get(player.getId()) > time) {
            COLLECTOR.wag(1.0F);
        }
        if (JITTER_UNTIL.get(player.getId()) > time) {
            COLLECTOR.jitter(CAFFEINE_JITTER);
        }
    }

    /** Addon sources in registration order, once the registry is frozen (after client init). */
    private static List<CosmeticReactionSource> sources() {
        if (sources == null) {
            ApiRegistry<CosmeticReactionSource> registry = FemboyClientApi.get().reactionSources();
            if (!registry.isFrozen()) {
                return List.of();
            }
            List<CosmeticReactionSource> list = new ArrayList<>();
            for (ResourceLocation id : registry.ids()) {
                registry.get(id).ifPresent(list::add);
            }
            sources = List.copyOf(list);
            sourceFailed = new boolean[list.size()];
        }
        return sources;
    }

    /** One reused sink: the strongest value of each reaction wins. */
    private static final class Collector implements CosmeticReactions {
        private @Nullable Entity noticed;
        private float noticeStrength;
        private float perk;
        private float flatten;
        private float droop;
        private float wag;
        private float puff;
        private float jitter;

        void reset() {
            noticed = null;
            noticeStrength = 0;
            perk = 0;
            flatten = 0;
            droop = 0;
            wag = 0;
            puff = 0;
            jitter = 0;
        }

        void applyTo(CosmeticRenderData data, Player wearer) {
            float yaw = noticed == null ? Float.NaN
                    : ReactionMath.worldYaw(noticed.getX() - wearer.getX(), noticed.getZ() - wearer.getZ());
            data.setReactionTargets(Math.max(perk, noticeStrength), yaw, flatten, droop, wag, puff, jitter);
            noticed = null; // don't keep the entity alive until the next evaluation
        }

        private static float unit(float amount) {
            return Mth.clamp(amount, 0.0F, 1.0F);
        }

        @Override
        public void notice(Entity entity, float strength) {
            float s = unit(strength);
            if (entity != null && s > noticeStrength) {
                noticed = entity;
                noticeStrength = s;
            }
        }

        @Override
        public void perk(float amount) {
            perk = Math.max(perk, unit(amount));
        }

        @Override
        public void flatten(float amount) {
            flatten = Math.max(flatten, unit(amount));
        }

        @Override
        public void droop(float amount) {
            droop = Math.max(droop, unit(amount));
        }

        @Override
        public void wag(float amount) {
            wag = Math.max(wag, unit(amount));
        }

        @Override
        public void puff(float amount) {
            puff = Math.max(puff, unit(amount));
        }

        @Override
        public void jitter(float amount) {
            jitter = Math.max(jitter, unit(amount));
        }
    }
}
