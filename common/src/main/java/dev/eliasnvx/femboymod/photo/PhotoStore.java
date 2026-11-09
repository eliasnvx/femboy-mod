package dev.eliasnvx.femboymod.photo;

import dev.eliasnvx.femboymod.FemboyMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
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

    private static final String EXTENSION = ".photo";
    /** Writes still running, by picture; {@link #delete} waits for them. */
    private static final Map<UUID, CompletableFuture<Void>> WRITES = new ConcurrentHashMap<>();

    private static Path folder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("femboymod").resolve("photos");
    }

    private static Path file(MinecraftServer server, UUID id) {
        return folder(server).resolve(id + EXTENSION);
    }

    public static void save(MinecraftServer server, UUID id, byte[] data) {
        synchronized (CACHE) {
            CACHE.put(id, data);
        }
        Path path = file(server, id);
        CompletableFuture<Void> write = CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, data);
            } catch (IOException e) {
                FemboyMod.LOGGER.warn("Couldn't save photo {}", id, e);
            }
        }, Util.ioPool());
        WRITES.put(id, write);
        write.whenComplete((ignored, error) -> WRITES.remove(id, write));
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

    /** Deletes a picture (it faded, {@link PhotoIndex}); photo items showing it stay blank. */
    public static CompletableFuture<Void> delete(MinecraftServer server, UUID id) {
        synchronized (CACHE) {
            CACHE.remove(id);
        }
        Path path = file(server, id);
        // the IO pool has several threads: a delete that overtook the picture's write would see no file, then the
        // write would bring it back
        CompletableFuture<Void> write = WRITES.getOrDefault(id, CompletableFuture.completedFuture(null));
        return write.exceptionally(error -> null).thenRunAsync(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                FemboyMod.LOGGER.warn("Couldn't delete photo {}", id, e);
            }
        }, Util.ioPool());
    }

    /**
     * The pictures on disk, oldest file first. Runs once per world, on the server thread, when {@link PhotoIndex} first
     * lists the pictures printed before it existed.
     */
    static List<UUID> storedOldestFirst(MinecraftServer server) {
        Path folder = folder(server);
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        record Stored(UUID id, FileTime time) {
        }
        List<Stored> stored = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path path : (Iterable<Path>) files::iterator) {
                String name = path.getFileName().toString();
                if (!name.endsWith(EXTENSION)) {
                    continue;
                }
                try {
                    UUID id = UUID.fromString(name.substring(0, name.length() - EXTENSION.length()));
                    stored.add(new Stored(id, Files.getLastModifiedTime(path)));
                } catch (IllegalArgumentException | IOException ignored) {
                    // not one of ours, or gone meanwhile
                }
            }
        } catch (IOException e) {
            FemboyMod.LOGGER.warn("Couldn't list photos", e);
        }
        stored.sort(Comparator.comparing(Stored::time));
        return stored.stream().map(Stored::id).toList();
    }

    /** Forgets cached pictures when the server stops (a new world may reuse nothing). */
    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
