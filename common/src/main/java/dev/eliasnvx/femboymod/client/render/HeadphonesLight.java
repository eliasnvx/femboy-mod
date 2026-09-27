package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/**
 * Light ring of the Cat-ear Headphones: soft pink, or a slow smooth rainbow while a jukebox plays near the local
 * player (checked once a second; never flashes, off with the client's RGB option).
 */
public final class HeadphonesLight {

    private static final int IDLE = 0xFFB6DA;
    private static final int CHECK_INTERVAL = 20;
    private static final int RANGE_CHUNKS = 1;
    private static final double RANGE_SQ = 16.0 * 16.0;
    private static final float HUE_PERIOD_TICKS = 100.0F;
    private static final float SATURATION = 0.55F;

    private static boolean musicNearby;
    private static int ticks;

    private HeadphonesLight() {
    }

    public static void tick(Minecraft minecraft) {
        if (++ticks % CHECK_INTERVAL != 0) {
            return;
        }
        musicNearby = false;
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockPos center = minecraft.player.blockPosition();
        ChunkPos chunk = new ChunkPos(center);
        for (int dx = -RANGE_CHUNKS; dx <= RANGE_CHUNKS && !musicNearby; dx++) {
            for (int dz = -RANGE_CHUNKS; dz <= RANGE_CHUNKS && !musicNearby; dz++) {
                for (BlockEntity entity : minecraft.level.getChunk(chunk.x + dx, chunk.z + dz).getBlockEntities().values()) {
                    if (entity instanceof JukeboxBlockEntity jukebox && jukebox.getSongPlayer().isPlaying()
                            && entity.getBlockPos().distSqr(center) <= RANGE_SQ) {
                        musicNearby = true;
                        break;
                    }
                }
            }
        }
    }

    public static int color() {
        if (!musicNearby || !FemboyConfig.client().rgbAnimations()) {
            return IDLE;
        }
        float hue = (ColorwayClock.ticks() % HUE_PERIOD_TICKS) / HUE_PERIOD_TICKS;
        return Mth.hsvToRgb(hue, SATURATION, 1.0F) & 0xFFFFFF;
    }
}
