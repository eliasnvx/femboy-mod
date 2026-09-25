package dev.eliasnvx.femboymod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.eliasnvx.femboymod.FemboyMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A JSON config file bound to a codec. Missing file → defaults are written. Broken file (bad JSON or
 * values) → logged, backed up as {@code .broken}, and replaced by defaults, so a typo never crashes the game.
 * Missing fields take their defaults (codecs use optionalFieldOf), so new options appear on upgrade.
 */
public final class ConfigFile<T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private final Codec<T> codec;
    private final T defaults;
    private volatile T value;

    public ConfigFile(Path path, Codec<T> codec, T defaults) {
        this.path = path;
        this.codec = codec;
        this.defaults = defaults;
        this.value = defaults;
    }

    public T get() {
        return value;
    }

    public T load() {
        if (!Files.exists(path)) {
            value = defaults;
            save(defaults);
            return value;
        }
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            value = codec.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalArgumentException::new);
            save(value); // rewrite with any newly added options filled in
        } catch (RuntimeException | IOException e) {
            FemboyMod.LOGGER.error("Config {} is invalid, using defaults (old file kept as .broken): {}", path, e.getMessage());
            try {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // best effort
            }
            value = defaults;
            save(defaults);
        }
        return value;
    }

    public void set(T newValue) {
        value = newValue;
        save(newValue);
    }

    private void save(T toSave) {
        try {
            Files.createDirectories(path.getParent());
            JsonElement json = codec.encodeStart(JsonOps.INSTANCE, toSave).getOrThrow(IllegalStateException::new);
            Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            FemboyMod.LOGGER.error("Could not write config {}", path, e);
        }
    }
}
