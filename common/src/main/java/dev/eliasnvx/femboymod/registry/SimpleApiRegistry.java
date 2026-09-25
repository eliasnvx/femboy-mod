package dev.eliasnvx.femboymod.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SimpleApiRegistry<T> implements ApiRegistry<T> {

    private final Identifier id;
    private final Map<Identifier, T> byId = new LinkedHashMap<>();
    private final Map<T, Identifier> byEntry = new IdentityHashMap<>();
    private final Codec<T> codec;
    private volatile boolean frozen;

    public SimpleApiRegistry(Identifier id) {
        this.id = id;
        this.codec = Identifier.CODEC.comapFlatMap(
                key -> get(key).map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown " + this.id + " entry: " + key)),
                entry -> getId(entry).orElseThrow(() -> new IllegalStateException("Unregistered " + this.id + " entry: " + entry)));
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public synchronized <V extends T> V register(Identifier key, V entry) {
        if (frozen) {
            throw new IllegalStateException("Registry " + id + " is frozen; register during FemboyAddon#onInitialize");
        }
        if (byId.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate " + id + " id: " + key);
        }
        if (byEntry.containsKey(entry)) {
            throw new IllegalArgumentException("Entry already registered in " + id + " as " + byEntry.get(entry));
        }
        byId.put(key, entry);
        byEntry.put(entry, key);
        return entry;
    }

    @Override
    public Optional<T> get(Identifier key) {
        return Optional.ofNullable(byId.get(key));
    }

    @Override
    public Optional<Identifier> getId(T entry) {
        return Optional.ofNullable(byEntry.get(entry));
    }

    @Override
    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    @Override
    public Codec<T> byIdCodec() {
        return codec;
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    public synchronized void freeze() {
        frozen = true;
    }
}
