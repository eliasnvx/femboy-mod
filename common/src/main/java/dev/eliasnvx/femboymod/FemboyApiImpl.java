package dev.eliasnvx.femboymod;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyDataComponents;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticsView;
import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.api.profile.ProfileField;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.profile.Profiles;
import dev.eliasnvx.femboymod.profile.StylePoints;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.effect.CosmeticCondition;
import dev.eliasnvx.femboymod.api.effect.CosmeticEffect;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import net.minecraft.world.entity.player.Player;
import java.util.Set;
import dev.eliasnvx.femboymod.event.FemboyEventBusImpl;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

public final class FemboyApiImpl implements FemboyApi {

    private static final String API_PROPERTIES = "/META-INF/femboymod-api.properties";

    private final String apiVersion = readApiVersion();
    private final FemboyEventBusImpl events = new FemboyEventBusImpl(FemboyMod.LOGGER);
    private final SimpleApiRegistry<CosmeticSlotType> cosmeticSlots =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "cosmetic_slot"));
    private final SimpleApiRegistry<MapCodec<? extends CosmeticEffect>> effectTypes =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "cosmetic_effect_type"));
    private final SimpleApiRegistry<MapCodec<? extends CosmeticCondition>> conditionTypes =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "cosmetic_condition_type"));
    private final SimpleApiRegistry<ProfileField<?>> profileFields =
            new SimpleApiRegistry<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "profile_field"));

    @Override
    public String apiVersion() {
        return apiVersion;
    }

    @Override
    public FemboyEventBus events() {
        return events;
    }

    @Override
    public ApiRegistry<CosmeticSlotType> cosmeticSlots() {
        return cosmeticSlots;
    }

    public SimpleApiRegistry<CosmeticSlotType> cosmeticSlotRegistry() {
        return cosmeticSlots;
    }

    @Override
    public FemboyDataComponents components() {
        return FemboyComponents.API;
    }

    @Override
    public CosmeticsView getCosmetics(LivingEntity entity) {
        return CosmeticsManager.get(entity);
    }

    @Override
    public Optional<Colorway> getColorway(ItemStack stack) {
        return Colorways.effective(stack);
    }

    @Override
    public ApiRegistry<MapCodec<? extends CosmeticEffect>> cosmeticEffectTypes() {
        return effectTypes;
    }

    @Override
    public ApiRegistry<MapCodec<? extends CosmeticCondition>> cosmeticConditionTypes() {
        return conditionTypes;
    }

    @Override
    public DripLevel getDripLevel(Player player) {
        return WornEvaluator.evaluate(player).drip();
    }

    @Override
    public Set<ResourceLocation> getActiveSetBonuses(Player player) {
        return WornEvaluator.evaluate(player).activeSets();
    }

    @Override
    public java.util.List<ItemStack> getCharms(ItemStack backpack) {
        if (!backpack.has(FemboyComponents.BACKPACK.get())) {
            return java.util.List.of();
        }
        return backpack.getOrDefault(FemboyComponents.CHARMS.get(), net.minecraft.world.item.component.ItemContainerContents.EMPTY)
                .nonEmptyStream().map(ItemStack::copy).toList();
    }

    @Override
    public ApiRegistry<ProfileField<?>> profileFields() {
        return profileFields;
    }

    @Override
    public PlayerProfile getProfile(Player player) {
        return Profiles.get(player);
    }

    @Override
    public boolean addStylePoints(ServerPlayer player, int amount, ResourceLocation reason) {
        return StylePoints.add(player, amount, reason);
    }

    void freezeRegistries() {
        profileFields.freeze();
        cosmeticSlots.freeze();
        effectTypes.freeze();
        conditionTypes.freeze();
    }

    private static String readApiVersion() {
        try (InputStream in = FemboyApi.class.getResourceAsStream(API_PROPERTIES)) {
            if (in == null) {
                throw new IllegalStateException(API_PROPERTIES + " is missing from the femboymod jar");
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("api_version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
