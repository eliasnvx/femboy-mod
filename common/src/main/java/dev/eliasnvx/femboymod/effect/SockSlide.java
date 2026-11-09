package dev.eliasnvx.femboymod.effect;

import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.entity.FemboyTriggers;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Sock sliding: in socks without shoes on a polished floor ({@code #femboymod:sock_slippery}) you keep your momentum,
 * like on a wooden floor at home. Block friction {@code f} becomes {@code 1 - (1 - f) * grip}, the math of 26.3's
 * {@code minecraft:friction_modifier} attribute. 1.21.1 has no such attribute, so the grip is a transient per-entity
 * value ({@link Slider}, not synced or saved) that {@code LivingEntitySlideMixin} applies to the friction
 * {@code LivingEntity#travel} reads from the floor.
 *
 * <p>Players move on their own client, so the client sets the grip for the local player every tick
 * ({@code SockSlideClient}); a server-side toggle would sync late and fight it. The server sets it for mobs
 * (Sock Goblins) and only listens to sliding players: a soft swish and an advancement. Sneaking grips.
 */
public final class SockSlide {

    /**
     * Ticks per window. Positions reach the server in packets that can bunch up (two in one tick, none in the next),
     * so speeds are compared as distances over two windows of this many ticks.
     */
    public static final int WINDOW = 4;
    /**
     * Coasting (no input) keeps between these shares of a window's distance in the next one: about 0.6 in socks on
     * planks or on ice; walking keeps ~1, stopping on a normal floor ~0.1.
     */
    private static final double COAST_MIN_KEEP = 0.35;
    private static final double COAST_MAX_KEEP = 0.85;
    /** Slower than this (blocks per tick, averaged over a window) is standing still. */
    private static final double MIN_SLIDE_SPEED = 0.03;
    /** The swish plays when a coast starts at least this fast, at most once per cooldown. */
    private static final double SWISH_SPEED = 0.08;
    private static final int SWISH_COOLDOWN_TICKS = 20;
    private static final float SWISH_VOLUME = 0.5F;
    private static final float SWISH_PITCH_SPREAD = 0.15F;
    /** One coast this long (blocks) earns the advancement. */
    private static final double DRIFT_DISTANCE = 0.8;

    /** Server: per sliding player, last position and the current coast. */
    private static final Map<ServerPlayer, Track> TRACKS = new WeakHashMap<>();

    private SockSlide() {
    }

    /** Friction after the grip, like 26.3's {@code LivingEntity#computeModifiedFriction}. */
    public static float effectiveFriction(float blockFriction, float grip) {
        return Mth.clamp(1.0F - (1.0F - blockFriction) * grip, 0.0F, 1.0F);
    }

    /**
     * Moving on without input: the distance fades slowly from one window to the next, the way it only does on a
     * slippery floor.
     *
     * @param previous distance covered in the previous {@link #WINDOW} ticks
     * @param recent   distance covered in the last {@link #WINDOW} ticks
     */
    public static boolean isCoasting(double previous, double recent) {
        return recent > MIN_SLIDE_SPEED * WINDOW && recent > previous * COAST_MIN_KEEP && recent < previous * COAST_MAX_KEEP;
    }

    /**
     * Over a polished floor, not sneaking (sneaking grips), not swimming, riding or gliding. Being on the ground isn't
     * required: vanilla ignores ground friction in the air, and keeping the grip through a hop saves churn. Vanilla
     * takes friction from the block under a carpet (carpet on ice is slippery), so a carpet or rug at the feet, or
     * anything else solid there that isn't polished, grips on its own.
     */
    public static boolean onSlipperyFloor(LivingEntity entity) {
        if (entity.isShiftKeyDown() || entity.isInWater() || entity.isPassenger() || entity.isFallFlying()) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        Level level = entity.level();
        if (!level.getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).is(FemboyTags.SOCK_SLIPPERY)) {
            return false;
        }
        BlockPos feet = entity.blockPosition();
        BlockState atFeet = level.getBlockState(feet);
        return atFeet.is(FemboyTags.SOCK_SLIPPERY) || atFeet.getCollisionShape(level, feet).isEmpty();
    }

    /**
     * The grip of the player's active {@code femboymod:slide} effects (the lowest wins), or 1 without any. Evaluated
     * from the worn outfit and conditions, the same on both sides.
     */
    public static float activeGrip(Player player) {
        float grip = 1.0F;
        for (WornEvaluator.PlannedEffect planned : WornEvaluator.evaluate(player).effects()) {
            if (planned.configured().effect() instanceof BuiltinEffects.SlideEffect slide
                    && planned.configured().when().map(c -> c.test(player)).orElse(true)) {
                grip = Math.min(grip, slide.grip());
            }
        }
        return grip;
    }

    /**
     * Sets how much grip the entity has (1 = normal, lower = slipperier); 1 or more is normal. A plain field write, so
     * calling it every tick is cheap.
     */
    public static void setGrip(LivingEntity entity, float grip) {
        ((Slider) entity).femboymod$setSlideGrip(Math.min(grip, 1.0F));
    }

    /** The entity's current grip (1 = normal). */
    public static float grip(LivingEntity entity) {
        return ((Slider) entity).femboymod$slideGrip();
    }

    /** Server, every tick while a slide effect is active on the player: the swish and the advancement. */
    static void listen(ServerPlayer player) {
        Track track = TRACKS.computeIfAbsent(player, p -> new Track(p.getX(), p.getZ()));
        double step = Math.sqrt(Mth.square(player.getX() - track.x) + Mth.square(player.getZ() - track.z));
        track.x = player.getX();
        track.z = player.getZ();
        track.steps[track.next] = step;
        track.next = (track.next + 1) % track.steps.length;
        double recent = 0;
        double previous = 0;
        for (int i = 0; i < WINDOW; i++) {
            recent += track.steps[(track.next + WINDOW + i) % track.steps.length];
            previous += track.steps[(track.next + i) % track.steps.length];
        }
        boolean coasting = player.onGround() && onSlipperyFloor(player) && isCoasting(previous, recent);
        if (coasting) {
            long now = player.level().getGameTime();
            if (track.coast == 0 && recent > SWISH_SPEED * WINDOW && now >= track.quietUntil) {
                float pitch = 1.0F + (player.getRandom().nextFloat() - 0.5F) * 2 * SWISH_PITCH_SPREAD;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), FemboySounds.SOCK_SLIDE.get(),
                        SoundSource.PLAYERS, SWISH_VOLUME, pitch);
                track.quietUntil = now + SWISH_COOLDOWN_TICKS;
            }
            // the window that revealed the coast was already part of it
            track.coast += track.coast == 0 ? recent : step;
            if (track.coast >= DRIFT_DISTANCE && !track.drifted) {
                track.drifted = true;
                FemboyTriggers.fire(player, FemboyTriggers.SOCK_DRIFT);
            }
        } else {
            track.coast = 0;
            track.drifted = false;
        }
    }

    static void forget(ServerPlayer player) {
        TRACKS.remove(player);
    }

    /**
     * Every {@link LivingEntity} implements this through {@code LivingEntitySlideMixin}: the stand-in for 26.3's
     * friction modifier. Use {@link #setGrip} and {@link #grip} instead of calling it directly.
     */
    public interface Slider {

        float femboymod$slideGrip();

        void femboymod$setSlideGrip(float grip);
    }

    private static final class Track {
        double x;
        double z;
        /** Distances of the last two windows, oldest first from {@code next}. */
        final double[] steps = new double[2 * WINDOW];
        int next;
        double coast;
        boolean drifted;
        long quietUntil;

        Track(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
}
