package dev.eliasnvx.femboymod.profile;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.api.profile.ProfileField;
import dev.eliasnvx.femboymod.network.ProfileSyncPayload;
import dev.eliasnvx.femboymod.platform.PlatformHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** {@link PlayerProfile} backed by the {@code femboymod:profile} player attachment. */
public final class Profiles {

    private Profiles() {
    }

    public static PlayerProfile get(Player player) {
        return new Access(player);
    }

    /** Synced fields only, for the owner's client. */
    public static CompoundTag syncedValues(Player player) {
        CompoundTag stored = PlatformHelper.getProfile(player).values();
        CompoundTag synced = new CompoundTag();
        for (var field : FemboyMod.api().profileFields().ids()) {
            FemboyMod.api().profileFields().get(field).filter(ProfileField::syncToOwner).ifPresent(f -> {
                Tag value = stored.get(f.id().toString());
                if (value != null) {
                    synced.put(f.id().toString(), value.copy());
                }
            });
        }
        return synced;
    }

    private record Access(Player player) implements PlayerProfile {

        @Override
        public <T> T get(ProfileField<T> field) {
            Tag value = PlatformHelper.getProfile(player).values().get(field.id().toString());
            if (value == null) {
                return field.defaultValue();
            }
            return field.codec().parse(NbtOps.INSTANCE, value).result().orElse(field.defaultValue());
        }

        @Override
        public <T> void set(ProfileField<T> field, T value) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                throw new IllegalStateException("Profiles are written on the server only");
            }
            if (!FemboyMod.api().profileFields().get(field.id()).map(registered -> registered == field).orElse(false)) {
                throw new IllegalStateException("Profile field not registered: " + field.id());
            }
            Tag encoded = field.codec().encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, error -> { });
            PlatformHelper.setProfile(player, PlatformHelper.getProfile(player).with(field.id().toString(), encoded));
            if (field.syncToOwner()) {
                CompoundTag update = new CompoundTag();
                update.put(field.id().toString(), encoded);
                ProfileSyncPayload.send(serverPlayer, update);
            }
        }
    }
}
