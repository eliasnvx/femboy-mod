package dev.eliasnvx.femboymod.api.registry;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.Set;

/**
 * A simple Java-side registry of API extension types (slot types, effect types, ...).
 *
 * <p>Registration is only allowed during {@link dev.eliasnvx.femboymod.api.FemboyAddon#onInitialize}.
 * After all addons have initialized the registry is frozen and further registration throws.
 * The contents must be identical on client and server (register from common code).
 *
 * @param <T> entry type
 */
public interface ApiRegistry<T> {

    /**
     * Returns this registry's id, for example {@code femboymod:cosmetic_slot}.
     *
     * @return the registry id
     */
    Identifier id();

    /**
     * Registers an entry.
     *
     * @param id    unique id, conventionally in your mod's namespace
     * @param entry the entry
     * @param <V>   entry subtype
     * @return {@code entry}, for convenient static field initialization
     * @throws IllegalStateException    if the registry is frozen
     * @throws IllegalArgumentException if {@code id} or {@code entry} is already registered
     */
    <V extends T> V register(Identifier id, V entry);

    /**
     * Looks up an entry by id.
     *
     * @param id the id
     * @return the entry, or empty if not registered
     */
    Optional<T> get(Identifier id);

    /**
     * Returns the id of a registered entry.
     *
     * @param entry the entry
     * @return the id, or empty if the entry is not registered
     */
    Optional<Identifier> getId(T entry);

    /**
     * Returns all registered ids in registration order.
     *
     * @return an immutable view of the ids
     */
    Set<Identifier> ids();

    /**
     * Returns a codec that (de)serializes entries by their id.
     *
     * @return the codec
     */
    Codec<T> byIdCodec();

    /**
     * Returns whether the registry is frozen.
     *
     * @return {@code true} once registration is closed
     */
    boolean isFrozen();
}
