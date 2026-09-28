package dev.eliasnvx.femboymod.api.profile;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A typed value stored in every player's {@link PlayerProfile}. Register fields in
 * {@link dev.eliasnvx.femboymod.api.FemboyApi#profileFields()} during
 * {@link dev.eliasnvx.femboymod.api.FemboyAddon#onInitialize}; the id must use your mod's namespace.
 *
 * <p>Values are saved with the player (kept on death) through {@code codec}. A value that fails to decode, or a
 * field that is not set yet, reads as {@code defaultValue}. Values of fields that are no longer registered (an
 * addon was removed) are kept untouched, so re-adding the addon restores them.
 *
 * <p>Example: a clan addon could register
 * {@code ProfileField.of(new ResourceLocation("myclans", "clan"), Codec.STRING, "", true)}.
 *
 * @param id           unique id, also the storage key
 * @param codec        how the value is saved
 * @param defaultValue value when nothing is stored; must be immutable
 * @param syncToOwner  whether the owning player's client receives the value (for HUDs and screens)
 * @param <T>          value type; must be immutable
 */
@ApiStatus.AvailableSince("0.1.0")
public record ProfileField<T>(ResourceLocation id, Codec<T> codec, T defaultValue, boolean syncToOwner) {

    /** Checks for nulls. */
    public ProfileField {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(defaultValue, "defaultValue");
    }

    /**
     * Creates a field.
     *
     * @param id           unique id
     * @param codec        value codec
     * @param defaultValue default value
     * @param syncToOwner  whether the owner's client gets the value
     * @param <T>          value type
     * @return the field (not registered yet)
     */
    public static <T> ProfileField<T> of(ResourceLocation id, Codec<T> codec, T defaultValue, boolean syncToOwner) {
        return new ProfileField<>(id, codec, defaultValue, syncToOwner);
    }
}
