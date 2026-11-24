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
     * Close enough to the spot above the bed's middle (y + 1.5); pets stand on the 3.8 px cushion (y + 0.24).
     * Even at a corner of the cushion (0.45 off on X/Z) the distance is ~1.41, while the nearest floor edge next
     * to the bed is >= 1.60 away.
     */
    private static final double REACH = 1.45;
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
        if (!(level.getBlockState(pos).getBlock() instanceof PetBedBlock) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        boolean alreadyHere = isReachedTarget() && pos.equals(blockPos);
        return pet.level().getEntitiesOfClass(TamableAnimal.class, new AABB(pos),
                other -> other != pet && (!alreadyHere || PetBedBlock.isResting(other))).isEmpty();
    }
}
