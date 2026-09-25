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

    /** Registers the Thrifter's clothing-rack POI (Fabric needs PoiHelper to map block states). */
    @ExpectPlatform
    public static void registerPoi() {
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
}
