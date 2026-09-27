package dev.eliasnvx.femboymod.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shape helpers that 1.21.1 lacks (26.3: {@code Shapes.rotateHorizontal}). */
public final class BlockShapes {

    private static final int QUARTER_TURNS = 4;

    private BlockShapes() {
    }

    /** {@code northShape} turned to face each horizontal direction (rotated around the block's vertical center line). */
    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape northShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int turns = Math.floorMod(facing.get2DDataValue() - Direction.NORTH.get2DDataValue(), QUARTER_TURNS);
            VoxelShape rotated = Shapes.empty();
            for (AABB box : northShape.toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.create(rotateClockwise(box, turns)));
            }
            shapes.put(facing, rotated.optimize());
        }
        return shapes;
    }

    /** Rotates a box inside the unit block {@code turns} quarter turns clockwise, seen from above (north -> east). */
    private static AABB rotateClockwise(AABB box, int turns) {
        AABB result = box;
        for (int i = 0; i < turns; i++) {
            // (x, z) -> (1 - z, x)
            result = new AABB(1.0 - result.maxZ, result.minY, result.minX, 1.0 - result.minZ, result.maxY, result.maxX);
        }
        return result;
    }
}
