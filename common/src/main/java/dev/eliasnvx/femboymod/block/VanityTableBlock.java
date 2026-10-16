package dev.eliasnvx.femboymod.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vanity Table: a dressing table with a mirror on its back edge, two blocks tall. The mirror is a real one (live
 * reflections, {@link dev.eliasnvx.femboymod.client.render.MirrorRenderer}), and right-click opens the Outfit screen.
 */
public class VanityTableBlock extends MirrorBlock {

    public static final MapCodec<VanityTableBlock> CODEC = simpleCodec(VanityTableBlock::new);

    /** Shapes with the mirror facing north: the table in the lower half, the mirror board in the upper one. */
    private static final Map<Direction, VoxelShape> TABLE = BlockShapes.rotateHorizontal(Block.box(0.0, 0.0, 3.0, 16.0, 12.0, 16.0));
    private static final Map<Direction, VoxelShape> MIRROR = BlockShapes.rotateHorizontal(Block.box(1.0, 0.0, 12.0, 15.0, 15.0, 16.0));

    public VanityTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<VanityTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? TABLE : MIRROR).get(state.getValue(FACING));
    }
}
