package dev.eliasnvx.femboymod.platform.forge;

import dev.architectury.platform.forge.EventBuses;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.DeferredRegister;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PlatformHelperImpl {

    /** Same key as Forge's package-private NetworkConstants.FML_NETVERSION (AttributeKey.valueOf returns the existing one). */
    private static final io.netty.util.AttributeKey<String> FML_NETVERSION = io.netty.util.AttributeKey.valueOf("fml:netversion");


    private static final Type ADDON_ANNOTATION = Type.getType(RegisterFemboyAddon.class);
    private static final DeferredRegister<PoiType> POIS = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, FemboyMod.MOD_ID);

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init(IEventBus modBus) {
        POIS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(FemboyPlayerData.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) -> {
            if (event.getObject() instanceof Player) {
                event.addCapability(FemboyPlayerData.ID, new FemboyPlayerData.Provider());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(PlatformHelperImpl::onClone);
        // Synced (network codec given) exactly where 1.21.1 synced them
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

    /**
     * Same semantics as the 1.21.1 attachments: everything is copied on a non-death clone (return from the End),
     * everything except the caffeine log on death. The old player's caps are already invalidated here, so they are
     * revived for the copy and invalidated again afterwards.
     */
    private static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            original.getCapability(FemboyPlayerData.CAPABILITY).ifPresent(old -> event.getEntity()
                    .getCapability(FemboyPlayerData.CAPABILITY)
                    .ifPresent(data -> data.copyFrom(old, event.isWasDeath())));
        } finally {
            original.invalidateCaps();
        }
    }

    public static List<DiscoveredAddon> discoverAddons() {
        List<DiscoveredAddon> result = new ArrayList<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            List<IModFileInfo> infos = scanData.getIModInfoData();
            String modId = infos.isEmpty() || infos.get(0).getMods().isEmpty()
                    ? "<unknown>"
                    : infos.get(0).getMods().get(0).getModId();
            scanData.getAnnotations().stream()
                    .filter(data -> data.targetType() == ElementType.TYPE && ADDON_ANNOTATION.equals(data.annotationType()))
                    .map(data -> data.clazz().getClassName())
                    .sorted(Comparator.naturalOrder())
                    .forEach(className -> {
                        ensureArchitecturyBus(modId);
                        FemboyAddon addon = instantiate(modId, className);
                        if (addon != null) {
                            result.add(new DiscoveredAddon(modId, addon));
                        }
                    });
        }
        return result;
    }

    public static CosmeticInventory getCosmetics(Player player) {
        return data(player, CosmeticInventory.EMPTY, d -> d.cosmetics);
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        update(player, d -> d.cosmetics = cosmetics);
    }

    /**
     * Addons run inside femboymod's constructor, usually before their own mod is constructed. Architectury 9's
     * DeferredRegister on Forge needs the owning mod's bus in {@link EventBuses}, which the addon could not have
     * registered yet; 1.21.1 (NeoForge) needed no such step, so femboymod does it for them to keep the addon API equal.
     */
    public static void ensureArchitecturyBus(String modId) {
        if (EventBuses.getModEventBus(modId).isPresent()) {
            return;
        }
        ModList.get().getModContainerById(modId)
                .filter(FMLModContainer.class::isInstance)
                .map(container -> ((FMLModContainer) container).getEventBus())
                .ifPresent(bus -> {
                    try {
                        EventBuses.registerModEventBus(modId, bus);
                    } catch (IllegalStateException alreadyRegistered) {
                        // The addon's own constructor registered it concurrently (mods are constructed in parallel)
                    }
                });
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
        return data(player, ProfileData.EMPTY, d -> d.profile);
    }

    public static void setProfile(Player player, ProfileData profile) {
        update(player, d -> d.profile = profile);
    }

    public static CaffeineLog getCaffeineLog(Player player) {
        return data(player, CaffeineLog.EMPTY, d -> d.caffeine);
    }

    public static void setCaffeineLog(Player player, CaffeineLog log) {
        update(player, d -> d.caffeine = log);
    }

    public static WardrobePresets getWardrobePresets(Player player) {
        return data(player, WardrobePresets.EMPTY, d -> d.presets);
    }

    public static void setWardrobePresets(Player player, WardrobePresets presets) {
        update(player, d -> d.presets = presets);
    }

    /** Forge maps POI block states itself when the POI registry is baked (GameData). */
    public static void registerPoi() {
        POIS.register(FemboyBlocks.THRIFTER_POI.location().getPath(), () -> new PoiType(
                Set.copyOf(FemboyBlocks.CLOTHING_RACK.get().getStateDefinition().getPossibleStates()), 1, 1));
    }

    /** Reads one value; a player without the capability (removed, caps invalidated) reads as {@code fallback}. */
    private static <T> T data(Player player, T fallback, Function<FemboyPlayerData, T> getter) {
        // orElse instead of map: getCosmetics runs every frame in CosmeticLayer, no Optional allocation
        FemboyPlayerData data = player.getCapability(FemboyPlayerData.CAPABILITY).orElse(null);
        return data == null ? fallback : getter.apply(data);
    }

    private static void update(Player player, Consumer<FemboyPlayerData> setter) {
        FemboyPlayerData data = player.getCapability(FemboyPlayerData.CAPABILITY).orElse(null);
        if (data != null) {
            setter.accept(data);
        }
    }

    public static boolean canReceive(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation id) {
        // Architectury 9 on Forge records the client's C2S ids as its receivables; any modded client has our channel.
        // Forge's isVanillaConnection throws without the FML version attribute (e.g. GameTest mock players): check it first.
        var channel = player.connection.connection.channel();
        return channel != null && channel.attr(FML_NETVERSION).get() != null
                && !net.minecraftforge.network.NetworkHooks.isVanillaConnection(player.connection.connection);
    }
}
