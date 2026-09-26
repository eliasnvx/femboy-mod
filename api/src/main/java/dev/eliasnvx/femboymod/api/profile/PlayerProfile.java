package dev.eliasnvx.femboymod.api.profile;

import java.util.function.UnaryOperator;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * Persistent per-player data shared by Femboy Mod and addons: statistics, Style Points, the cosmetics
 * collection and any {@link ProfileField} an addon registers. Obtain it with
 * {@link dev.eliasnvx.femboymod.api.FemboyApi#getProfile(Player)}.
 *
 * <p>The server is authoritative: writes are only allowed on the logical server and are sent to the owner's
 * client for fields with {@link ProfileField#syncToOwner()}. On the client, reads return the last synced values
 * (defaults for fields that are not synced). The profile is kept on death.
 *
 * <p>Change Style Points through {@link dev.eliasnvx.femboymod.api.FemboyApi#addStylePoints} rather than
 * {@link #set}, so the {@link dev.eliasnvx.femboymod.api.event.profile.StylePointsEvent} fires.
 */
@ApiStatus.AvailableSince("0.1.0")
public interface PlayerProfile {

    /**
     * @return the owner
     */
    Player player();

    /**
     * Reads a field.
     *
     * @param field the field
     * @param <T>   value type
     * @return the stored value, or the field's default
     */
    <T> T get(ProfileField<T> field);

    /**
     * Writes a field (logical server only).
     *
     * @param field the field
     * @param value the new value, not {@code null}
     * @param <T>   value type
     * @throws IllegalStateException on the client, or if the field is not registered
     */
    <T> void set(ProfileField<T> field, T value);

    /**
     * Reads, transforms and writes a field (logical server only).
     *
     * @param field    the field
     * @param function maps the old value to the new one
     * @param <T>      value type
     * @return the new value
     */
    default <T> T update(ProfileField<T> field, UnaryOperator<T> function) {
        T value = function.apply(get(field));
        set(field, value);
        return value;
    }
}
