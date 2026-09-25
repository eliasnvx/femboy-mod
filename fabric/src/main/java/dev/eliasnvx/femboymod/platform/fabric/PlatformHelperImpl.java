package dev.eliasnvx.femboymod.platform.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlatformHelperImpl {

    private static final AttachmentType<CosmeticInventory> COSMETICS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "cosmetics"),
            builder -> builder
                    .initializer(() -> CosmeticInventory.EMPTY)
                    .persistent(CosmeticInventory.CODEC)
                    .copyOnDeath());

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init() {
        DynamicRegistries.registerSynced(ColorwayPattern.REGISTRY_KEY, ColorwayPattern.CODEC);
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

    public static CosmeticInventory getCosmetics(Player player) {
        return player.getAttachedOrElse(COSMETICS, CosmeticInventory.EMPTY);
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        player.setAttached(COSMETICS, cosmetics);
    }
}
