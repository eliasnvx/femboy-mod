package dev.eliasnvx.femboymod.platform.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import dev.eliasnvx.femboymod.energy.CaffeineLog;
import dev.eliasnvx.femboymod.energy.CaffeineRules;
import dev.eliasnvx.femboymod.energy.EnergyDrink;
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

    private static final AttachmentType<CaffeineLog> CAFFEINE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "caffeine"),
            builder -> builder.initializer(() -> CaffeineLog.EMPTY).persistent(CaffeineLog.CODEC));

    private static final AttachmentType<WardrobePresets> PRESETS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "wardrobe_presets"),
            builder -> builder.initializer(() -> WardrobePresets.EMPTY).persistent(WardrobePresets.CODEC).copyOnDeath());

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init() {
        DynamicRegistries.registerSynced(ColorwayPattern.REGISTRY_KEY, ColorwayPattern.CODEC);
        DynamicRegistries.registerSynced(CosmeticStats.REGISTRY_KEY, CosmeticStats.CODEC);
        DynamicRegistries.registerSynced(SetBonus.REGISTRY_KEY, SetBonus.CODEC);
        DynamicRegistries.registerSynced(DripRules.REGISTRY_KEY, DripRules.CODEC);
        DynamicRegistries.register(EnergyDrink.REGISTRY_KEY, EnergyDrink.CODEC);
        DynamicRegistries.register(CaffeineRules.REGISTRY_KEY, CaffeineRules.CODEC);
        DynamicRegistries.register(dev.eliasnvx.femboymod.api.combat.DripDamage.REGISTRY_KEY, dev.eliasnvx.femboymod.api.combat.DripDamage.CODEC);
        DynamicRegistries.registerSynced(dev.eliasnvx.femboymod.api.backpack.CharmStats.REGISTRY_KEY, dev.eliasnvx.femboymod.api.backpack.CharmStats.CODEC);
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

    public static CaffeineLog getCaffeineLog(Player player) {
        return player.getAttachedOrElse(CAFFEINE, CaffeineLog.EMPTY);
    }

    public static void setCaffeineLog(Player player, CaffeineLog log) {
        player.setAttached(CAFFEINE, log);
    }

    public static WardrobePresets getWardrobePresets(Player player) {
        return player.getAttachedOrElse(PRESETS, WardrobePresets.EMPTY);
    }

    public static void setWardrobePresets(Player player, WardrobePresets presets) {
        player.setAttached(PRESETS, presets);
    }

    public static void registerPoi() {
        net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper.register(FemboyBlocks.THRIFTER_POI.identifier(), 1, 1,
                FemboyBlocks.CLOTHING_RACK.get());
    }
}
