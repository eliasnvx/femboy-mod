package dev.eliasnvx.femboymod.platform.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.RegisterFemboyAddon;
import dev.eliasnvx.femboymod.api.colorway.ColorwayPattern;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
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
                    .serialize(CosmeticInventory.CODEC.fieldOf("items"))
                    .copyOnDeath()
                    .build());

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> {
            event.dataPackRegistry(ColorwayPattern.REGISTRY_KEY, ColorwayPattern.CODEC, ColorwayPattern.CODEC);
            event.dataPackRegistry(CosmeticStats.REGISTRY_KEY, CosmeticStats.CODEC, CosmeticStats.CODEC);
            event.dataPackRegistry(SetBonus.REGISTRY_KEY, SetBonus.CODEC, SetBonus.CODEC);
            event.dataPackRegistry(DripRules.REGISTRY_KEY, DripRules.CODEC, DripRules.CODEC);
        });
    }

    public static List<DiscoveredAddon> discoverAddons() {
        List<DiscoveredAddon> result = new ArrayList<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            List<IModFileInfo> infos = scanData.getIModInfoData();
            String modId = infos.isEmpty() || infos.getFirst().getMods().isEmpty()
                    ? "<unknown>"
                    : infos.getFirst().getMods().getFirst().getModId();
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
}
