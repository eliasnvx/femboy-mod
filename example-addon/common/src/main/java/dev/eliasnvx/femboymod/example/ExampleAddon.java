package dev.eliasnvx.femboymod.example;

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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference addon that exercises the public API (SPEC §8.5). It is built in CI, so any
 * breaking API change fails the build here first.
 *
 * <p>Uses: a new cosmetic slot type, a new cosmetic item (data component), a colorway pattern
 * from JSON ({@code data/femboymod_example/femboymod/colorway/candy.json}) and two event listeners.
 * Discovered via the "femboymod" entrypoint on Fabric and {@link RegisterFemboyAddon} on NeoForge.
 */
@RegisterFemboyAddon
public final class ExampleAddon implements FemboyAddon {

    public static final String MOD_ID = "femboymod_example";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier PIN_SLOT = id("pin");
    /** Just after the built-in slots (which use multiples of 100). */
    private static final int PIN_SLOT_ORDER = 1000;

    /** femboymod's creative tab, referenced by key so the addon does not touch femboymod internals. */
    private static final ResourceKey<CreativeModeTab> FEMBOYMOD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(FemboyApi.MOD_ID, "main"));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);

    /** The addon's cosmetic item; set during {@link #onInitialize}. */
    public static RegistrySupplier<Item> friendshipPin;

    @Override
    public void onInitialize(FemboyApi api) {
        api.cosmeticSlots().register(PIN_SLOT, new CosmeticSlotType(PIN_SLOT_ORDER));

        ResourceKey<Item> pinKey = ResourceKey.create(Registries.ITEM, id("friendship_pin"));
        RegistrySupplier<Item> pin = friendshipPin = ITEMS.register(pinKey.identifier(), () -> new Item(new Item.Properties()
                .setId(pinKey)
                .stacksTo(1)
                .arch$tab(FEMBOYMOD_TAB)
                .component(api.components().cosmetic().get(), new Cosmetic(PIN_SLOT))));
        ITEMS.register();

        api.events().addListener(CosmeticChangedEvent.class, event ->
                LOGGER.info("{} changed slot {}: {} -> {}", event.entity().getName().getString(), event.slot(),
                        event.previous(), event.current()));
        api.events().addListener(CosmeticUnequipEvent.class, event ->
                LOGGER.info("{} took off {}", event.entity().getName().getString(), event.removed()));

        LOGGER.info("femboymod example addon initialized against API {} (item {})", api.apiVersion(), pin.getId());
    }

    @Override
    public void onInitializeClient(FemboyClientApi api) {
        LOGGER.info("femboymod example addon client initialized");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
