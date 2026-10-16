package dev.eliasnvx.femboymod.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Loader-specific services. Implementations: {@code platform.fabric/neoforge.PlatformHelperImpl}. */
public final class PlatformHelper {

    private PlatformHelper() {
    }

    /** Finds and instantiates every {@link dev.eliasnvx.femboymod.api.FemboyAddon} provided by installed mods. */
    /**
     * Whether {@code player}'s client can receive our S2C packet {@code id}. On Forge 1.20.1 Architectury 9's own
     * check never passes for S2C-only ids (its handshake reports the client's C2S ids), so Forge checks for a modded
     * connection instead.
     */
    @ExpectPlatform
    public static boolean canReceive(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<DiscoveredAddon> discoverAddons() {
        throw new AssertionError();
    }

    /** Reads the player's cosmetics attachment (persistent, copied on death). Never null. */
    @ExpectPlatform
    public static CosmeticInventory getCosmetics(Player player) {
        throw new AssertionError();
    }

    /** Replaces the player's cosmetics attachment. Does not sync; callers handle networking. */
    @ExpectPlatform
    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        throw new AssertionError();
    }

    /** Saved wardrobe outfits (persistent, kept on death). */
    @ExpectPlatform
    public static dev.eliasnvx.femboymod.wardrobe.WardrobePresets getWardrobePresets(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setWardrobePresets(Player player, dev.eliasnvx.femboymod.wardrobe.WardrobePresets presets) {
        throw new AssertionError();
    }

    /**
     * Builds the printed photo item. Fabric registers its custom renderer separately (BuiltinItemRendererRegistry);
     * Forge 47 can only attach one to the item itself ({@code Item#initializeClient}), so it returns a subclass.
     */
    @ExpectPlatform
    public static dev.eliasnvx.femboymod.item.PhotoItem createPhotoItem(net.minecraft.world.item.Item.Properties properties) {
        throw new AssertionError();
    }

    /** Registers the POIs: the Thrifter's clothing rack, the laundry basket (Fabric needs PoiHelper to map block states). */
    @ExpectPlatform
    public static void registerPoi() {
        throw new AssertionError();
    }

    /**
     * Game day a villager last gave out Halloween candy (-1 = never); saved with the villager. Fabric: a persistent
     * attachment; Forge 47: the entity's persistent data.
     */
    @ExpectPlatform
    public static long getLastTreatDay(net.minecraft.world.entity.Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setLastTreatDay(net.minecraft.world.entity.Entity entity, long day) {
        throw new AssertionError();
    }

    /** Recent energy drinks (persistent, reset on death). Never null. */
    @ExpectPlatform
    public static dev.eliasnvx.femboymod.energy.CaffeineLog getCaffeineLog(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setCaffeineLog(Player player, dev.eliasnvx.femboymod.energy.CaffeineLog log) {
        throw new AssertionError();
    }

    /** Player profile (persistent, kept on death). Never null. */
    @ExpectPlatform
    public static dev.eliasnvx.femboymod.profile.ProfileData getProfile(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setProfile(Player player, dev.eliasnvx.femboymod.profile.ProfileData profile) {
        throw new AssertionError();
    }
}
