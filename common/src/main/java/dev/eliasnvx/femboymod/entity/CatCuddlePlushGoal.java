package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.block.PlushBlock;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Tamed cats (and Stray Cats) occasionally curl up next to a nearby {@link PlushBlock}, purring and emitting a heart. */
public class CatCuddlePlushGoal extends Goal {

    private static final int SEARCH_RADIUS = 6;
    private static final int SEARCH_HEIGHT = 2;
    private static final int CHECK_INTERVAL = 80;
    private static final int COOLDOWN_TICKS = 400;
    private static final int CUDDLE_DURATION = 200;
    private static final int PURR_INTERVAL = 70;
    private static final double SPEED = 0.85;
    private static final double ARRIVE_DIST_SQ = 1.6 * 1.6;

    private final Cat cat;
    private @Nullable BlockPos plushPos;
    private @Nullable BlockPos restPos;
    private long nextCheck;
    private int cuddleTicks;
    private int walkTicks;

    public CatCuddlePlushGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!cat.isTame() || cat.isOrderedToSit() || cat.isLying()) {
            return false;
        }
        if (!(cat.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        if (now < nextCheck) {
            return false;
        }
        nextCheck = now + CHECK_INTERVAL;
        return findPlush(level);
    }

    @Override
    public boolean canContinueToUse() {
        if (plushPos == null || restPos == null || cat.isOrderedToSit() || !(cat.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!(level.getBlockState(plushPos).getBlock() instanceof PlushBlock)) {
            return false;
        }
        if (cuddleTicks > 0) {
            return cuddleTicks < CUDDLE_DURATION;
        }
        return walkTicks < 160 && !cat.getNavigation().isDone();
    }

    @Override
    public void start() {
        cuddleTicks = 0;
        walkTicks = 0;
        if (restPos != null) {
            cat.getNavigation().moveTo(restPos.getX() + 0.5, restPos.getY(), restPos.getZ() + 0.5, SPEED);
        }
    }

    @Override
    public void stop() {
        cat.setLying(false);
        cat.getNavigation().stop();
        plushPos = null;
        restPos = null;
        cuddleTicks = 0;
        if (cat.level() instanceof ServerLevel level) {
            nextCheck = level.getGameTime() + COOLDOWN_TICKS;
        }
    }

    @Override
    public void tick() {
        if (plushPos == null || restPos == null) {
            return;
        }
        if (cuddleTicks == 0) {
            walkTicks++;
            Vec3 center = Vec3.atBottomCenterOf(restPos);
            if (cat.position().distanceToSqr(center) <= ARRIVE_DIST_SQ) {
                cat.getNavigation().stop();
                cat.setLying(true);
                cuddleTicks = 1;
                purr();
            }
            return;
        }
        cuddleTicks++;
        cat.getLookControl().setLookAt(plushPos.getX() + 0.5, plushPos.getY() + 0.35, plushPos.getZ() + 0.5);
        if (cuddleTicks % PURR_INTERVAL == 0) {
            purr();
        }
    }

    private void purr() {
        if (cat.level() instanceof ServerLevel level) {
            cat.playAmbientSound();
            level.sendParticles(ParticleTypes.HEART, cat.getX(), cat.getY() + 0.55, cat.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
    }

    private boolean findPlush(ServerLevel level) {
        BlockPos origin = cat.blockPosition();
        BlockPos bestPlush = null;
        BlockPos bestRest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, -SEARCH_HEIGHT, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS))) {
            if (!(level.getBlockState(pos).getBlock() instanceof PlushBlock)) {
                continue;
            }
            BlockPos spot = adjacentSpot(level, pos);
            if (spot == null) {
                continue;
            }
            double dist = pos.distSqr(origin);
            if (dist < bestDist) {
                bestDist = dist;
                bestPlush = pos.immutable();
                bestRest = spot;
            }
        }
        this.plushPos = bestPlush;
        this.restPos = bestRest;
        return bestPlush != null;
    }

    private static @Nullable BlockPos adjacentSpot(ServerLevel level, BlockPos plush) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = plush.relative(dir);
            if (level.getBlockState(side).getCollisionShape(level, side).isEmpty()
                    && !level.getBlockState(side.below()).getCollisionShape(level, side.below()).isEmpty()) {
                return side.immutable();
            }
        }
        return null;
    }
}
