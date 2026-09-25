package dev.eliasnvx.femboymod;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyDataComponents;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticsView;
import dev.eliasnvx.femboymod.api.event.FemboyEventBus;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.event.FemboyEventBusImpl;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.SimpleApiRegistry;
import net.minecraft.resources.Identifier;
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
            new SimpleApiRegistry<>(Identifier.fromNamespaceAndPath(MOD_ID, "cosmetic_slot"));

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
        return Optional.ofNullable(stack.get(FemboyComponents.COLORWAY.get()));
    }

    void freezeRegistries() {
        cosmeticSlots.freeze();
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
