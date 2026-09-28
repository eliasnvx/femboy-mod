package dev.eliasnvx.femboymod.platform.forge;

import com.mojang.serialization.Codec;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.energy.CaffeineLog;
import dev.eliasnvx.femboymod.profile.ProfileData;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent per-player data on Forge 47, the capability counterpart of the NeoForge attachments of 1.21.1.
 * Cosmetics, wardrobe presets and the profile survive death; the caffeine log is reset on death.
 */
public final class FemboyPlayerData {

    /** Capability id on the player ({@code femboymod:player_data}). */
    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "player_data");

    public static final Capability<FemboyPlayerData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static final String COSMETICS_KEY = "cosmetics";
    private static final String CAFFEINE_KEY = "caffeine";
    private static final String PRESETS_KEY = "wardrobe_presets";
    private static final String PROFILE_KEY = "profile";

    CosmeticInventory cosmetics = CosmeticInventory.EMPTY;
    CaffeineLog caffeine = CaffeineLog.EMPTY;
    WardrobePresets presets = WardrobePresets.EMPTY;
    ProfileData profile = ProfileData.EMPTY;

    /** Copies data from the old player entity on respawn or on return from the End. */
    void copyFrom(FemboyPlayerData old, boolean wasDeath) {
        cosmetics = old.cosmetics;
        presets = old.presets;
        profile = old.profile;
        // The caffeine log has no copy-on-death, like the 1.21.1 attachment: only kept on non-death clones
        if (!wasDeath) {
            caffeine = old.caffeine;
        }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        write(tag, COSMETICS_KEY, CosmeticInventory.CODEC, cosmetics);
        write(tag, CAFFEINE_KEY, CaffeineLog.CODEC, caffeine);
        write(tag, PRESETS_KEY, WardrobePresets.CODEC, presets);
        write(tag, PROFILE_KEY, ProfileData.CODEC, profile);
        return tag;
    }

    void load(CompoundTag tag) {
        cosmetics = read(tag, COSMETICS_KEY, CosmeticInventory.CODEC, CosmeticInventory.EMPTY);
        caffeine = read(tag, CAFFEINE_KEY, CaffeineLog.CODEC, CaffeineLog.EMPTY);
        presets = read(tag, PRESETS_KEY, WardrobePresets.CODEC, WardrobePresets.EMPTY);
        profile = read(tag, PROFILE_KEY, ProfileData.CODEC, ProfileData.EMPTY);
    }

    private static <T> void write(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(error -> FemboyMod.LOGGER.error("Failed to save player {}: {}", key, error))
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    private static <T> T read(CompoundTag tag, String key, Codec<T> codec, T fallback) {
        if (!tag.contains(key)) {
            return fallback;
        }
        Tag encoded = tag.get(key);
        return codec.parse(NbtOps.INSTANCE, encoded)
                .resultOrPartial(error -> FemboyMod.LOGGER.error("Failed to load player {}: {}", key, error))
                .orElse(fallback);
    }

    /** Attached to every player (both logical sides); saved with the player NBT. */
    static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final FemboyPlayerData data = new FemboyPlayerData();
        // Not invalidated on removal: PlayerEvent.Clone revives the old player's caps to read them
        private final LazyOptional<FemboyPlayerData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }
    }
}
