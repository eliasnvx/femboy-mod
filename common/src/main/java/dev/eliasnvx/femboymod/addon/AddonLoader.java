package dev.eliasnvx.femboymod.addon;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.platform.PlatformHelper;

import java.util.List;

/** Discovers addons through the platform and calls their init hooks, isolating failures. */
public final class AddonLoader {

    private static List<DiscoveredAddon> addons = List.of();

    private AddonLoader() {
    }

    public static void initCommon(FemboyApi api) {
        addons = List.copyOf(PlatformHelper.discoverAddons());
        for (DiscoveredAddon addon : addons) {
            try {
                addon.addon().onInitialize(api);
                FemboyMod.LOGGER.info("Loaded femboymod addon {} ({})", addon.modId(), addon.addon().getClass().getName());
            } catch (Throwable t) {
                FemboyMod.LOGGER.error("femboymod addon {} failed in onInitialize", addon.modId(), t);
            }
        }
    }

    public static void initClient(FemboyClientApi api) {
        for (DiscoveredAddon addon : addons) {
            try {
                addon.addon().onInitializeClient(api);
            } catch (Throwable t) {
                FemboyMod.LOGGER.error("femboymod addon {} failed in onInitializeClient", addon.modId(), t);
            }
        }
    }

    public static int addonCount() {
        return addons.size();
    }

    /** An addon instance and the id of the mod that provided it. */
    public record DiscoveredAddon(String modId, FemboyAddon addon) {
    }
}
