package dev.eliasnvx.femboymod.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;

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
}
