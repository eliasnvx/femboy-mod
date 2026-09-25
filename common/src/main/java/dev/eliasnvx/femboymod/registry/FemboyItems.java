package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class FemboyItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    /** First wearable; full set of SPEC §5.1 items arrives in Phase 2. */
    public static final RegistrySupplier<Item> CAT_EARS = register("cat_ears",
            props -> new Item(props.stacksTo(1).component(FemboyComponents.COSMETIC.get(), new Cosmetic(FemboySlots.HEAD_ACCESSORY))));

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabRegistry.create(
            Component.translatable("itemGroup.femboymod"), () -> new ItemStack(CAT_EARS.get())));

    private FemboyItems() {
    }

    private static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
        return REGISTER.register(name, () -> factory.apply(new Item.Properties().setId(key).arch$tab(TAB)));
    }
}
