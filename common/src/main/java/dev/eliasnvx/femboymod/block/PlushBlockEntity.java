package dev.eliasnvx.femboymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight, non-ticking block entity for plushies; holds only transient client-side squish timing
 * triggered via vanilla block events (no saved data, no inventory).
 */
public class PlushBlockEntity extends BlockEntity {

    public static final int EVENT_CLICK = 1;
    public static final int EVENT_PLACE = 2;

    private long squishStartTick = Long.MIN_VALUE;
    private @Nullable PlushSquish.Kind squishKind;

    public PlushBlockEntity(BlockPos pos, BlockState state) {
        super(FemboyBlocks.PLUSH_ENTITY.get(), pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_CLICK) {
            startSquish(PlushSquish.Kind.CLICK);
            return true;
        }
        if (id == EVENT_PLACE) {
            startSquish(PlushSquish.Kind.PLACE);
            return true;
        }
        return super.triggerEvent(id, param);
    }

    private void startSquish(PlushSquish.Kind kind) {
        this.squishKind = kind;
        this.squishStartTick = level != null ? level.getGameTime() : 0L;
    }

    public @Nullable PlushSquish.Kind squishKind() {
        return squishKind;
    }

    /** Ticks elapsed since the last squish started, or {@code -1} if idle. */
    public float squishAge(float partialTicks) {
        if (squishKind == null || level == null || squishStartTick == Long.MIN_VALUE) {
            return -1.0F;
        }
        return (float) (level.getGameTime() - squishStartTick) + partialTicks;
    }
}
