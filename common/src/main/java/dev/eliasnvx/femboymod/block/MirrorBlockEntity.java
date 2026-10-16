package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lower half of a mirror; holds no data, it only gives the client renderer a place to draw reflections. */
public class MirrorBlockEntity extends BlockEntity {

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.MIRROR_ENTITY.get(), pos, state);
    }
}
