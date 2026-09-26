package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Sends a Wandering Cosplayer to a random player in the overworld now and then (config
 * {@code friends.cosplayer_*}); at most one is around at a time.
 */
public final class CosplayerSpawner {

    private static final int MIN_DISTANCE = 16;
    private static final int MAX_DISTANCE = 32;
    private static final int ATTEMPTS = 10;

    private CosplayerSpawner() {
    }

    public static void tick(MinecraftServer server) {
        CommonConfig.Friends config = FemboyConfig.common().friends();
        ServerLevel level = server.overworld();
        if (config.cosplayerCheckInterval() <= 0 || level.getGameTime() % config.cosplayerCheckInterval() != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= config.cosplayerChance() || !level.getEntities(FemboyEntities.COSPLAYER.get(), c -> true).isEmpty()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (!players.isEmpty()) {
            spawnNear(level, players.get(random.nextInt(players.size())), config.cosplayerStayTicks());
        }
    }

    /** Places a cosplayer on the surface 16..32 blocks from the player; returns null if no spot was found. */
    public static Cosplayer spawnNear(ServerLevel level, ServerPlayer player, int stayTicks) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < ATTEMPTS; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.getBlockState(pos.below()).isSolid() || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
                continue;
            }
            Cosplayer cosplayer = FemboyEntities.COSPLAYER.get().create(level, EntitySpawnReason.EVENT);
            if (cosplayer != null) {
                cosplayer.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                cosplayer.setDespawnDelay(stayTicks);
                cosplayer.setWanderTarget(player.blockPosition());
                level.addFreshEntity(cosplayer);
                return cosplayer;
            }
        }
        return null;
    }
}
