package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.block.PetBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

/**
 * A tamed cat or dog walks to a free Pet Bed nearby and rests on it for a while: cats lie down, dogs sit. Not while
 * ordered to sit, and not when the owner has wandered off. Resting there heals ({@link PetBedBlock}). Cats and wolves
 * get it through {@code PetBedGoalsMixin}.
 */
public class PetBedGoal extends MoveToBlockGoal {

    private static final double SPEED = 1.0;
    private static final int SEARCH_RANGE = 8;
    private static final int VERTICAL_RANGE = 4;
    /** Ticks between looks for a bed; vanilla cats look every 2 s too. */
    private static final int LOOK_INTERVAL = 40;
    /**
     * Close enough to the spot above the bed's middle; pets stand on the 9 px cushion. Navigation stops a pet within
     * 0.45 of the bed's node on both axes, so one that comes in diagonally can stop at a corner of the cushion, about
     * 1.13 from the spot: 1.2 takes it (the floor next to the bed is 1.5 away and still doesn't count).
     */
    private static final double REACH = 1.2;
    private static final double OWNER_RANGE = 12.0;

    private final TamableAnimal pet;

    public PetBedGoal(TamableAnimal pet) {
        super(pet, SPEED, SEARCH_RANGE, VERTICAL_RANGE);
        this.pet = pet;
    }

    @Override
    public boolean canUse() {
        return pet.isTame() && !pet.isOrderedToSit() && !pet.isLeashed() && ownerNear() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !pet.isOrderedToSit() && ownerNear() && super.canContinueToUse();
    }

    private boolean ownerNear() {
        LivingEntity owner = pet.getOwner();
        return owner == null || pet.distanceToSqr(owner) < OWNER_RANGE * OWNER_RANGE;
    }

    @Override
    protected int nextStartTick(PathfinderMob mob) {
        return LOOK_INTERVAL;
    }

    @Override
    public double acceptedDistance() {
        return REACH;
    }

    @Override
    public void start() {
        super.start();
        rest(false);
    }

    @Override
    public void tick() {
        super.tick();
        rest(isReachedTarget());
    }

    @Override
    public void stop() {
        super.stop();
        if (!pet.isOrderedToSit()) {
            rest(false);
        }
    }

    /** Cats lie down on the bed, other pets sit. */
    private void rest(boolean resting) {
        if (pet instanceof Cat cat) {
            cat.setInSittingPose(false);
            cat.setLying(resting);
        } else {
            pet.setInSittingPose(resting);
        }
    }

    /** A pet bed with room above it that no other pet is on (two pets on one bed push each other off). */
    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof PetBedBlock && level.isEmptyBlock(pos.above())
                && pet.level().getEntitiesOfClass(TamableAnimal.class, new AABB(pos), other -> other != pet).isEmpty();
    }
}
