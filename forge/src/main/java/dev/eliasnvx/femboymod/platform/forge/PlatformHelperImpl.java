package dev.eliasnvx.femboymod.platform.forge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.RegisterFemboyAddon;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import dev.eliasnvx.femboymod.energy.CaffeineLog;
import dev.eliasnvx.femboymod.profile.ProfileData;
import dev.eliasnvx.femboymod.energy.CaffeineRules;
import dev.eliasnvx.femboymod.energy.EnergyDrink;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class PlatformHelperImpl {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FemboyMod.MOD_ID);

    private static final Supplier<AttachmentType<CosmeticInventory>> COSMETICS = ATTACHMENTS.register("cosmetics",
            () -> AttachmentType.builder(() -> CosmeticInventory.EMPTY)
                    .serialize(CosmeticInventory.CODEC.fieldOf("items").codec())
                    .copyOnDeath()
                    .build());

    private static final Supplier<AttachmentType<CaffeineLog>> CAFFEINE = ATTACHMENTS.register("caffeine",
            () -> AttachmentType.builder(() -> CaffeineLog.EMPTY).serialize(CaffeineLog.CODEC.fieldOf("drinks").codec()).build());

    private static final Supplier<AttachmentType<WardrobePresets>> PRESETS = ATTACHMENTS.register("wardrobe_presets",
            () -> AttachmentType.builder(() -> WardrobePresets.EMPTY).serialize(WardrobePresets.CODEC.fieldOf("presets").codec()).copyOnDeath().build());
    private static final Supplier<AttachmentType<ProfileData>> PROFILE = ATTACHMENTS.register("profile",
            () -> AttachmentType.builder(() -> ProfileData.EMPTY).serialize(ProfileData.CODEC.fieldOf("values").codec()).copyOnDeath().build());
    private static final DeferredRegister<PoiType> POIS = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, FemboyMod.MOD_ID);

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        POIS.register(modBus);
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> {
            event.dataPackRegistry(ColorwayPattern.REGISTRY_KEY, ColorwayPattern.CODEC, ColorwayPattern.CODEC);
            event.dataPackRegistry(CosmeticStats.REGISTRY_KEY, CosmeticStats.CODEC, CosmeticStats.CODEC);
            event.dataPackRegistry(SetBonus.REGISTRY_KEY, SetBonus.CODEC, SetBonus.CODEC);
            event.dataPackRegistry(DripRules.REGISTRY_KEY, DripRules.CODEC, DripRules.CODEC);
            event.dataPackRegistry(EnergyDrink.REGISTRY_KEY, EnergyDrink.CODEC);
            event.dataPackRegistry(dev.eliasnvx.femboymod.food.BubbleTeaFlavor.REGISTRY_KEY, dev.eliasnvx.femboymod.food.BubbleTeaFlavor.CODEC);
            event.dataPackRegistry(CaffeineRules.REGISTRY_KEY, CaffeineRules.CODEC);
            event.dataPackRegistry(dev.eliasnvx.femboymod.api.combat.DripDamage.REGISTRY_KEY, dev.eliasnvx.femboymod.api.combat.DripDamage.CODEC);
            event.dataPackRegistry(dev.eliasnvx.femboymod.api.backpack.CharmStats.REGISTRY_KEY, dev.eliasnvx.femboymod.api.backpack.CharmStats.CODEC, dev.eliasnvx.femboymod.api.backpack.CharmStats.CODEC);
        });
    }

    public static List<DiscoveredAddon> discoverAddons() {
        List<DiscoveredAddon> result = new ArrayList<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            List<IModFileInfo> infos = scanData.getIModInfoData();
            String modId = infos.isEmpty() || infos.get(0).getMods().isEmpty()
                    ? "<unknown>"
                    : infos.get(0).getMods().get(0).getModId();
            scanData.getAnnotatedBy(RegisterFemboyAddon.class, ElementType.TYPE)
                    .map(data -> data.clazz().getClassName())
                    .sorted(Comparator.naturalOrder())
                    .forEach(className -> {
                        FemboyAddon addon = instantiate(modId, className);
                        if (addon != null) {
                            result.add(new DiscoveredAddon(modId, addon));
                        }
                    });
        }
        return result;
    }

    public static CosmeticInventory getCosmetics(Player player) {
        return player.getData(COSMETICS);
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        player.setData(COSMETICS, cosmetics);
    }

    private static FemboyAddon instantiate(String modId, String className) {
        try {
            Class<?> clazz = Class.forName(className, true, PlatformHelperImpl.class.getClassLoader());
            if (!FemboyAddon.class.isAssignableFrom(clazz)) {
                FemboyMod.LOGGER.error("{} (mod {}) is annotated with @RegisterFemboyAddon but does not implement FemboyAddon",
                        className, modId);
                return null;
            }
            return (FemboyAddon) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            FemboyMod.LOGGER.error("Failed to instantiate femboymod addon {} from mod {}", className, modId, e);
            return null;
        }
    }

    public static ProfileData getProfile(Player player) {
        return player.getData(PROFILE);
    }

    public static void setProfile(Player player, ProfileData profile) {
        player.setData(PROFILE, profile);
    }

    public static CaffeineLog getCaffeineLog(Player player) {
        return player.getData(CAFFEINE);
    }

    public static void setCaffeineLog(Player player, CaffeineLog log) {
        player.setData(CAFFEINE, log);
    }

    public static WardrobePresets getWardrobePresets(Player player) {
        return player.getData(PRESETS);
    }

    public static void setWardrobePresets(Player player, WardrobePresets presets) {
        player.setData(PRESETS, presets);
    }

    /** NeoForge maps POI block states automatically on registration. */
    public static void registerPoi() {
        POIS.register(FemboyBlocks.THRIFTER_POI.location().getPath(), () -> new PoiType(
                java.util.Set.copyOf(FemboyBlocks.CLOTHING_RACK.get().getStateDefinition().getPossibleStates()), 1, 1));
    }
}
