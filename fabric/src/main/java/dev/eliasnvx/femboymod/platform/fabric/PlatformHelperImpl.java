package dev.eliasnvx.femboymod.platform.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;
import java.util.List;

public final class PlatformHelperImpl {

    private PlatformHelperImpl() {
    }

    public static List<DiscoveredAddon> discoverAddons() {
        List<DiscoveredAddon> result = new ArrayList<>();
        for (EntrypointContainer<FemboyAddon> container
                : FabricLoader.getInstance().getEntrypointContainers(FemboyAddon.FABRIC_ENTRYPOINT, FemboyAddon.class)) {
            String modId = container.getProvider().getMetadata().getId();
            try {
                result.add(new DiscoveredAddon(modId, container.getEntrypoint()));
            } catch (Throwable t) {
                FemboyMod.LOGGER.error("Failed to instantiate femboymod addon from mod {}", modId, t);
            }
        }
        return result;
    }
}
