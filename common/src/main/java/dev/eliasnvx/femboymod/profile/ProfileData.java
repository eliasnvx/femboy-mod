package dev.eliasnvx.femboymod.profile;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/**
 * Stored form of a player profile: one compound, field id → encoded value. Unknown ids (fields of removed
 * addons) are kept as they are. Treat {@link #values()} as read-only; changes go through {@link #with}.
 */
public record ProfileData(CompoundTag values) {

    public static final ProfileData EMPTY = new ProfileData(new CompoundTag());
    public static final Codec<ProfileData> CODEC = CompoundTag.CODEC.xmap(ProfileData::new, ProfileData::values);

    /** A copy with one value replaced. */
    public ProfileData with(String key, net.minecraft.nbt.Tag value) {
        CompoundTag copy = values.copy();
        copy.put(key, value);
        return new ProfileData(copy);
    }

    /** A copy with the given values merged over this one (used by the client when a sync arrives). */
    public ProfileData merge(CompoundTag update) {
        CompoundTag copy = values.copy();
        for (String key : update.keySet()) {
            copy.put(key, update.get(key).copy());
        }
        return new ProfileData(copy);
    }
}
