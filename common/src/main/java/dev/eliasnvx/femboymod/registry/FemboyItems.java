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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class FemboyItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    // SPEC §5.1. Balance lives in data/femboymod/femboymod/cosmetic_stats/<item>.json, not here.
    public static final RegistrySupplier<Item> CAT_EARS = cosmetic("cat_ears", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> TAIL = cosmetic("tail", FemboySlots.TAIL);
    public static final RegistrySupplier<Item> OVERSIZED_HOODIE = cosmetic("oversized_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> PLEATED_SKIRT = cosmetic("pleated_skirt", FemboySlots.OUTFIT_BOTTOM);
    public static final RegistrySupplier<Item> PROGRAMMING_SOCKS = cosmetic("programming_socks", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> FISHNET_TIGHTS = cosmetic("fishnet_tights", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> UWU_CHOKER = cosmetic("uwu_choker", FemboySlots.NECK);

    /** Hair clip shapes (SPEC §5.1: "10 forms"); all share the hair_clip renderer. */
    public static final List<String> HAIR_CLIP_SHAPES = List.of(
            "heart", "star", "bow", "flower", "moon", "cherry", "bunny", "fish", "lightning", "butterfly");
    public static final List<RegistrySupplier<Item>> HAIR_CLIPS = HAIR_CLIP_SHAPES.stream()
            .map(shape -> cosmetic("hair_clip_" + shape, new Cosmetic(FemboySlots.HEAD_ACCESSORY,
                    Optional.of(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "hair_clip")))))
            .toList();

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabRegistry.create(
            Component.translatable("itemGroup.femboymod"), () -> new ItemStack(CAT_EARS.get())));

    private FemboyItems() {
    }

    private static RegistrySupplier<Item> cosmetic(String name, Identifier slot) {
        return cosmetic(name, new Cosmetic(slot));
    }

    private static RegistrySupplier<Item> cosmetic(String name, Cosmetic cosmetic) {
        return register(name, props -> new Item(props.stacksTo(1).component(FemboyComponents.COSMETIC.get(), cosmetic)));
    }

    private static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
        return REGISTER.register(name, () -> factory.apply(new Item.Properties().setId(key).arch$tab(TAB)));
    }
}
