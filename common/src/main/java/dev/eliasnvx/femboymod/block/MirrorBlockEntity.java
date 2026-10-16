package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Lower half of a mirror; holds no data, it only gives the client renderer a place to draw reflections. */
public class MirrorBlockEntity extends BlockEntity {

    /** The mirror is two blocks tall; reflections are drawn inside it. */
    private static final int HEIGHT = 2;

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.MIRROR_ENTITY.get(), pos, state);
    }

    /**
     * Frustum culling box on Forge, where this overrides {@code IForgeBlockEntity#getRenderBoundingBox} (by default
     * the collision box of the lower half only, so the mirror would vanish while its upper half is still in view).
     * Vanilla and Fabric don't call it.
     */
    public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(pos, pos.offset(1, HEIGHT, 1));
    }
}
