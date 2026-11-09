package dev.eliasnvx.femboymod.platform.fabric;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.addon.AddonLoader.DiscoveredAddon;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsEvents;
import dev.eliasnvx.femboymod.network.SyncedRegistries;
import dev.eliasnvx.femboymod.block.FemboyBlocks;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import dev.eliasnvx.femboymod.energy.CaffeineLog;
import dev.eliasnvx.femboymod.profile.ProfileData;
import dev.eliasnvx.femboymod.energy.CaffeineRules;
import dev.eliasnvx.femboymod.energy.EnergyDrink;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class PlatformHelperImpl {

    // Fabric API 0.92 ships the (experimental) Data Attachment API v1.0.x: builder() + buildAndRegister(id).
    // Persistent attachments are saved into the player's NBT; copyOnDeath ones survive respawn, the rest
    // survive only the End-exit "respawn" (same semantics as 1.21.1).
    private static final AttachmentType<CosmeticInventory> COSMETICS = AttachmentRegistry.<CosmeticInventory>builder()
            .initializer(() -> CosmeticInventory.EMPTY)
            .persistent(CosmeticInventory.CODEC)
            .copyOnDeath()
            .buildAndRegister(new ResourceLocation(FemboyMod.MOD_ID, "cosmetics"));

    private static final AttachmentType<CaffeineLog> CAFFEINE = AttachmentRegistry.<CaffeineLog>builder()
            .initializer(() -> CaffeineLog.EMPTY)
            .persistent(CaffeineLog.CODEC)
            .buildAndRegister(new ResourceLocation(FemboyMod.MOD_ID, "caffeine"));

    private static final AttachmentType<WardrobePresets> PRESETS = AttachmentRegistry.<WardrobePresets>builder()
            .initializer(() -> WardrobePresets.EMPTY)
            .persistent(WardrobePresets.CODEC)
            .copyOnDeath()
            .buildAndRegister(new ResourceLocation(FemboyMod.MOD_ID, "wardrobe_presets"));

    private static final AttachmentType<ProfileData> PROFILE = AttachmentRegistry.<ProfileData>builder()
            .initializer(() -> ProfileData.EMPTY)
            .persistent(ProfileData.CODEC)
            .copyOnDeath()
            .buildAndRegister(new ResourceLocation(FemboyMod.MOD_ID, "profile"));

    /** Halloween: the game day a villager last gave out candy (saved with the villager, see TrickOrTreat). */
    private static final AttachmentType<Long> TREAT_DAY = AttachmentRegistry.<Long>builder()
            .initializer(() -> -1L)
            .persistent(com.mojang.serialization.Codec.LONG)
            .buildAndRegister(new ResourceLocation(FemboyMod.MOD_ID, "treat_day"));

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link FemboyMod#init()}. */
    public static void init() {
        SyncedRegistries.ALL.forEach(PlatformHelperImpl::registerSynced);
        DynamicRegistries.register(EnergyDrink.REGISTRY_KEY, EnergyDrink.CODEC);
        DynamicRegistries.register(dev.eliasnvx.femboymod.food.BubbleTeaFlavor.REGISTRY_KEY, dev.eliasnvx.femboymod.food.BubbleTeaFlavor.CODEC);
        DynamicRegistries.register(CaffeineRules.REGISTRY_KEY, CaffeineRules.CODEC);
        DynamicRegistries.register(dev.eliasnvx.femboymod.api.combat.DripDamage.REGISTRY_KEY, dev.eliasnvx.femboymod.api.combat.DripDamage.CODEC);
        // right after the vanilla death loot (0.92 fires it in ServerPlayer#die after dropAllDeathLoot), and only for
        // a death that happened: a death another mod cancels keeps the outfit. Fabric has no shared drops event, so
        // grave mods only collect these if they support the mod.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                CosmeticsEvents.dropOnDeath(player);
            }
        });
    }

    private static <T> void registerSynced(SyncedRegistries.Entry<T> entry) {
        DynamicRegistries.registerSynced(entry.key(), entry.codec(), entry.networkCodec());
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

    public static ProfileData getProfile(Player player) {
        return player.getAttachedOrElse(PROFILE, ProfileData.EMPTY);
    }

    public static void setProfile(Player player, ProfileData profile) {
        player.setAttached(PROFILE, profile);
    }

    public static long getLastTreatDay(net.minecraft.world.entity.Entity entity) {
        return entity.getAttachedOrElse(TREAT_DAY, -1L);
    }

    public static void setLastTreatDay(net.minecraft.world.entity.Entity entity, long day) {
        entity.setAttached(TREAT_DAY, day);
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

    /** Fabric registers the photo's renderer from the client entrypoint (BuiltinItemRendererRegistry). */
    public static dev.eliasnvx.femboymod.item.PhotoItem createPhotoItem(net.minecraft.world.item.Item.Properties properties) {
        return new dev.eliasnvx.femboymod.item.PhotoItem(properties);
    }

    public static void registerPoi() {
        net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper.register(FemboyBlocks.THRIFTER_POI.location(), 1, 1,
                FemboyBlocks.CLOTHING_RACK.get());
        net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper.register(FemboyBlocks.LAUNDRY_BASKET_POI.location(), 0, 1,
                FemboyBlocks.LAUNDRY_BASKET.get());
    }

    public static boolean canReceive(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation id) {
        return dev.architectury.networking.NetworkManager.canPlayerReceive(player, id);
    }

    public static boolean canMobGrief(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity mob) {
        return level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
    }
}
