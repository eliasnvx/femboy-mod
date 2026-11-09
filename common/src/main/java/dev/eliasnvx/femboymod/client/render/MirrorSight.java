package dev.eliasnvx.femboymod.client.render;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * What a mirror can "see": an entity is reflected only when no wall stands between the mirror's front and the entity
 * (its eyes or its middle), so a mirror never shows players in the next room. The raycasts run at most once per tick
 * per mirror and entity, not every frame. Client only.
 */
final class MirrorSight {

    /** How far in front of the mirror block the sight line starts (clear of the mirror's own frame). */
    private static final double FRONT_OFFSET = 0.55;
    /** Mirrors forget entities they haven't looked at for this long (they went away or the mirror did). */
    private static final long FORGET_TICKS = 200;

    private static final class Entry {
        long tick = Long.MIN_VALUE;
        final Int2BooleanOpenHashMap seen = new Int2BooleanOpenHashMap();
    }

    private static final Long2ObjectOpenHashMap<Entry> BY_MIRROR = new Long2ObjectOpenHashMap<>();
    private static Level lastLevel;

    private MirrorSight() {
    }

    /**
     * Whether the mirror at {@code mirror} (facing {@code nx}/{@code nz}, glass centered at {@code glassY}) has a clear
     * line to {@code target}.
     */
    static boolean sees(Level level, BlockPos mirror, double nx, double nz, double glassY, Entity target) {
        if (level != lastLevel) {
            BY_MIRROR.clear();
            lastLevel = level;
        }
        long now = level.getGameTime();
        Entry entry = BY_MIRROR.get(mirror.asLong());
        if (entry == null) {
            if (BY_MIRROR.size() > 64) {
                BY_MIRROR.values().removeIf(old -> now - old.tick > FORGET_TICKS);
            }
            entry = new Entry();
            BY_MIRROR.put(mirror.asLong(), entry);
        }
        if (entry.tick != now) {
            entry.tick = now;
            entry.seen.clear();
        }
        int id = target.getId();
        if (entry.seen.containsKey(id)) {
            return entry.seen.get(id);
        }
        Vec3 from = new Vec3(mirror.getX() + 0.5 + nx * FRONT_OFFSET, glassY, mirror.getZ() + 0.5 + nz * FRONT_OFFSET);
        boolean visible = clear(level, from, target.getEyePosition())
                || clear(level, from, target.position().add(0, target.getBbHeight() / 2, 0));
        entry.seen.put(id, visible);
        return visible;
    }

    private static boolean clear(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }
}
