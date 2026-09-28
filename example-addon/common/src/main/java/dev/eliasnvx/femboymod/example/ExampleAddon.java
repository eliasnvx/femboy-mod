package dev.eliasnvx.femboymod.example;

import dev.eliasnvx.femboymod.api.event.profile.VibeCheckEvent;
import dev.eliasnvx.femboymod.api.profile.ProfileField;
import dev.eliasnvx.femboymod.api.event.profile.StylePointsEvent;
import dev.eliasnvx.femboymod.api.event.profile.SetupRatedEvent;
import dev.eliasnvx.femboymod.api.event.profile.EnergyDrinkEvent;
import dev.eliasnvx.femboymod.api.event.profile.CriticReviewEvent;
import dev.eliasnvx.femboymod.api.event.profile.CollectionUnlockEvent;
import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.api.FemboyAddon;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.RegisterFemboyAddon;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.CosmeticUnequipEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Reference addon that exercises the public API (SPEC §8.5). It is built in CI, so any
 * breaking API change fails the build here first.
 *
 * <p>Uses: a new cosmetic slot type, a new cosmetic item (data component) with its own renderer,
 * a custom effect and condition type ({@link ExampleEffects}), and two event listeners. Data pack side
 * ({@code data/femboymod_example/femboymod/}): colorway patterns ({@code colorway/candy.json}, shimmering
 * {@code colorway/candy_shimmer.json}), a set
 * bonus ({@code set_bonus/friendship.json}), charm stats for the pin ({@code charm/friendship_pin.json},
 * the pin is also in {@code #femboymod:charms}) and cosmetic stats ({@code cosmetic_stats/friendship_pin.json}).
 * Discovered via the "femboymod" entrypoint on Fabric and {@link RegisterFemboyAddon} on NeoForge.
 */
@RegisterFemboyAddon
public final class ExampleAddon implements FemboyAddon {

    public static final String MOD_ID = "femboymod_example";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceLocation PIN_SLOT = id("pin");
    /** Custom renderer id (client side: {@link ExamplePinRenderer}). */
    public static final ResourceLocation PIN_RENDERER = id("pin");
    /** Just after the built-in slots (which use multiples of 100). */
    private static final int PIN_SLOT_ORDER = 1000;

    /** femboymod's creative tab, referenced by key so the addon does not touch femboymod internals. */
    private static final ResourceKey<CreativeModeTab> FEMBOYMOD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            new ResourceLocation(FemboyApi.MOD_ID, "main"));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);

    /**
     * Addon data in the player profile: one friendship point per newly collected cosmetic. Synced, so a client
     * screen could show it.
     */
    public static final ProfileField<Integer> FRIENDSHIP_POINTS = ProfileField.of(id("friendship_points"), Codec.INT, 0, true);
    /** Extra Style Points per payout while the friendship pin is worn. */
    public static final int PIN_STYLE_BONUS = 1;
    /** Extra vibe at the Vibe Check Scanner while the friendship pin is worn. */
    public static final int PIN_VIBE_BONUS = 5;

    /** The addon's cosmetic item; set during {@link #onInitialize}. */
    public static RegistrySupplier<Item> friendshipPin;

    @Override
    public void onInitialize(FemboyApi api) {
        api.cosmeticSlots().register(PIN_SLOT, new CosmeticSlotType(PIN_SLOT_ORDER));
        ExampleEffects.register(api);

        RegistrySupplier<Item> pin = friendshipPin = ITEMS.register(id("friendship_pin"), () -> new Item(new Item.Properties()
                .stacksTo(1)
                .arch$tab(FEMBOYMOD_TAB)
                .component(api.components().cosmetic().get(), new Cosmetic(PIN_SLOT, Optional.of(PIN_RENDERER)))));
        ITEMS.register();

        api.events().addListener(CosmeticChangedEvent.class, event ->
                LOGGER.info("{} changed slot {}: {} -> {}", event.entity().getName().getString(), event.slot(),
                        event.previous(), event.current()));
        // Player profile + Style Points + gameplay events (API 0.1.0)
        api.profileFields().register(FRIENDSHIP_POINTS.id(), FRIENDSHIP_POINTS);
        api.events().addListener(CollectionUnlockEvent.class, event ->
                api.getProfile(event.player()).update(FRIENDSHIP_POINTS, points -> points + 1));
        api.events().addListener(StylePointsEvent.class, event -> event.setAmount(withPinBonus(event.amount(),
                api.getCosmetics(event.player()).get(PIN_SLOT).is(pin.get()))));
        api.events().addListener(CriticReviewEvent.class, event ->
                LOGGER.info("critic reviewed {}: tier {}, impressed {}", event.player().getName().getString(), event.dripTier(), event.impressed()));
        api.events().addListener(SetupRatedEvent.class, event ->
                LOGGER.info("{} rated their setup {}/{}", event.player().getName().getString(), event.stars(), event.maxStars()));
        // Friendship pin wearers get a +5 vibe bonus at the Vibe Check Scanner
        api.events().addListener(VibeCheckEvent.class, event -> {
            if (api.getCosmetics(event.player()).get(PIN_SLOT).is(pin.get())) {
                event.setScore(event.score() + PIN_VIBE_BONUS);
            }
        });
        api.events().addListener(EnergyDrinkEvent.class, event ->
                LOGGER.info("{} drank {} ({} recently)", event.player().getName().getString(), event.drink(), event.recentDrinks()));
        api.events().addListener(CosmeticUnequipEvent.class, event ->
                LOGGER.info("{} took off {}", event.entity().getName().getString(), event.removed()));

        LOGGER.info("femboymod example addon initialized against API {} (item {})", api.apiVersion(), pin.getId());
    }

    /** The pin adds {@link #PIN_STYLE_BONUS} to earned Style Points; spending is not touched. */
    public static int withPinBonus(int amount, boolean wearingPin) {
        return amount > 0 && wearingPin ? amount + PIN_STYLE_BONUS : amount;
    }

    @Override
    public void onInitializeClient(FemboyClientApi api) {
        ExamplePinRenderer.register(api);
        LOGGER.info("femboymod example addon client initialized");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
