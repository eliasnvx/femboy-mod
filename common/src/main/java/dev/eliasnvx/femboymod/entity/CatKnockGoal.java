package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The cat meme: now and then, while a player is watching, a cat walks up to a flower pot, candle or plushie at the
 * edge of a table, stares at the player, and pushes it off ({@link CatMischief}). Not while ordered to sit or lying
 * down; the owner (if any) is the preferred audience. Cats and Stray Cats get it through {@code PetBedGoalsMixin};
 * numbers in config {@code cats.knock_*}. A cat in the mischief part of a catnip high ({@link Catnip}) looks much
 * more often, always goes, and barely rests in between ({@code plants.catnip_knock_*}).
 */
public class CatKnockGoal extends Goal {

    private static final double SPEED = 1.0;
    /** Close enough to the block to swat it: navigation can stop a cat about 1.1 off a node (see PetBedGoal). */
    private static final double REACH = 1.4;
    private static final double REACH_HEIGHT = 0.6;
    /** Give up if the walk and the stare take longer than this. */
    private static final int TIMEOUT_TICKS = 400;
    private static final float LOOK_SPEED = 30.0F;
    /** Degrees per tick the cat turns its body toward the player while staring. */
    private static final float STARE_TURN = 15.0F;
    /** After the stare, the cat turns to the block for this many ticks before the push. */
    private static final int WINDUP_TICKS = 6;

    private final Cat cat;
    private final Supplier<CommonConfig.Cats> config;
    private @Nullable CatMischief.Target target;
    private @Nullable Player audience;
    private long lastCheck = Long.MIN_VALUE / 2;
    private long lastKnock = Long.MIN_VALUE / 2;
    private int stare;
    private int windup;
    private int timeout;

    public CatKnockGoal(Cat cat) {
        this(cat, () -> FemboyConfig.common().cats());
    }

    /** With its own numbers (GameTests use a quick, certain cat). */
    public CatKnockGoal(Cat cat, Supplier<CommonConfig.Cats> config) {
        this.cat = cat;
        this.config = config;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(cat.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        CommonConfig.Cats config = this.config.get();
        CommonConfig.Plants catnip = FemboyConfig.common().plants();
        boolean high = Catnip.phase(cat) == Catnip.Phase.MISCHIEF;
        int interval = high ? catnip.catnipKnockInterval() : config.knockCheckInterval();
        int cooldown = high ? catnip.catnipKnockCooldownTicks() : config.knockCooldownTicks();
        if (now - lastCheck < interval || now - lastKnock < cooldown) {
            return false;
        }
        lastCheck = now;
        if (cat.isOrderedToSit() || cat.isInSittingPose() || cat.isLying() || cat.isLeashed() || cat.isPassenger()
                || !CatMischief.allowed(level, cat) || (!high && cat.getRandom().nextFloat() >= config.knockChance())) {
            return false;
        }
        audience = findAudience(level, config.knockAudienceRange());
        if (audience == null) {
            return false;
        }
        target = CatMischief.find(level, cat.blockPosition(), config.knockSearchRadius());
        return target != null;
    }

    /** The owner if they're close and can see the cat, otherwise the nearest player who can. */
    private @Nullable Player findAudience(ServerLevel level, double range) {
        LivingEntity owner = cat.getOwner();
        if (owner instanceof Player player && watches(player, range)) {
            return player;
        }
        return level.getNearestPlayer(cat.getX(), cat.getY(), cat.getZ(), range,
                entity -> entity instanceof Player player && watches(player, range));
    }

    private boolean watches(Player player, double range) {
        return !player.isSpectator() && player.isAlive() && player.distanceToSqr(cat) <= range * range && player.hasLineOfSight(cat);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && audience != null && audience.isAlive() && timeout > 0 && !cat.isOrderedToSit()
                && CatMischief.stillKnockable(cat.level(), target);
    }

    @Override
    public void start() {
        stare = config.get().knockStareTicks();
        windup = WINDUP_TICKS;
        timeout = TIMEOUT_TICKS;
        walk();
    }

    @Override
    public void stop() {
        target = null;
        audience = null;
        cat.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || audience == null || !(cat.level() instanceof ServerLevel level)) {
            return;
        }
        timeout--;
        Vec3 block = Vec3.atBottomCenterOf(target.block());
        double dx = cat.getX() - block.x;
        double dz = cat.getZ() - block.z;
        boolean close = dx * dx + dz * dz <= REACH * REACH
                && Math.abs(cat.getY() - block.y) < REACH_HEIGHT;
        if (!close) {
            if (cat.getNavigation().isDone()) {
                walk();
            }
            return;
        }
        cat.getNavigation().stop();
        if (stare > 0) {
            stare--; // the look before the push: the whole cat turns to its audience, not just the head
            cat.lookAt(audience, STARE_TURN, LOOK_SPEED);
            cat.setYBodyRot(cat.getYRot());
            cat.getLookControl().setLookAt(audience, LOOK_SPEED, LOOK_SPEED);
            return;
        }
        if (windup > 0) {
            windup--; // turn to the block, then swat
            float yaw = (float) (Mth.atan2(block.z - cat.getZ(), block.x - cat.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            cat.setYRot(Mth.approachDegrees(cat.getYRot(), yaw, STARE_TURN * 2));
            cat.setYBodyRot(cat.getYRot());
            cat.getLookControl().setLookAt(block.x, block.y, block.z);
            return;
        }
        CatMischief.knock(level, target, cat, audience);
        lastKnock = level.getGameTime();
        target = null;
    }

    private void walk() {
        if (target != null) {
            Vec3 stand = Vec3.atBottomCenterOf(target.stand());
            cat.getNavigation().moveTo(stand.x, stand.y, stand.z, SPEED);
        }
    }
}
