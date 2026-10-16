package dev.eliasnvx.femboymod.photo;

import dev.eliasnvx.femboymod.FemboyMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Printed photos on disk: {@code <world>/femboymod/photos/<id>.photo}, the {@link PhotoCodec} bytes. Reads and writes
 * run on the IO pool; the last few pictures stay in memory because many players tend to look at the same ones.
 */
public final class PhotoStore {

    private static final int CACHE_SIZE = 32;
    private static final Map<UUID, byte[]> CACHE = new LinkedHashMap<>(CACHE_SIZE, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private PhotoStore() {
    }

    private static Path file(MinecraftServer server, UUID id) {
        return server.getWorldPath(LevelResource.ROOT).resolve("femboymod").resolve("photos").resolve(id + ".photo");
    }

    public static void save(MinecraftServer server, UUID id, byte[] data) {
        synchronized (CACHE) {
            CACHE.put(id, data);
        }
        Path path = file(server, id);
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, data);
            } catch (IOException e) {
                FemboyMod.LOGGER.warn("Couldn't save photo {}", id, e);
            }
        });
    }

    public static CompletableFuture<Optional<byte[]>> load(MinecraftServer server, UUID id) {
        synchronized (CACHE) {
            byte[] cached = CACHE.get(id);
            if (cached != null) {
                return CompletableFuture.completedFuture(Optional.of(cached));
            }
        }
        Path path = file(server, id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.isRegularFile(path) || Files.size(path) > PhotoCodec.MAX_BYTES) {
                    return Optional.<byte[]>empty();
                }
                byte[] data = Files.readAllBytes(path);
                synchronized (CACHE) {
                    CACHE.put(id, data);
                }
                return Optional.of(data);
            } catch (IOException e) {
                FemboyMod.LOGGER.warn("Couldn't read photo {}", id, e);
                return Optional.<byte[]>empty();
            }
        }, Util.ioPool());
    }

    /** Forgets cached pictures when the server stops (a new world may reuse nothing). */
    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
