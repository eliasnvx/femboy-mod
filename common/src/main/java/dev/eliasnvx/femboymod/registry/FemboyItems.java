package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import net.minecraft.core.registries.Registries;
import dev.eliasnvx.femboymod.energy.EmptyCanBlock;
import dev.eliasnvx.femboymod.energy.EnergyDrinkItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.nbt.CompoundTag;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import dev.eliasnvx.femboymod.backpack.BackpackItem;
import dev.eliasnvx.femboymod.backpack.BackpackSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class FemboyItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.ITEM);
    /** Registration order = creative tab order (see {@link CreativeTabContents}). */
    static final List<RegistrySupplier<Item>> TAB_ORDER = new ArrayList<>();
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    // SPEC §5.1. Balance lives in data/femboymod/femboymod/cosmetic_stats/<item>.json, not here.
    public static final RegistrySupplier<Item> CAT_EARS = cosmetic("cat_ears", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> TAIL = cosmetic("tail", FemboySlots.TAIL);
    public static final RegistrySupplier<Item> FOX_EARS = cosmetic("fox_ears", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> BUNNY_EARS = cosmetic("bunny_ears", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> BEAR_EARS = cosmetic("bear_ears", FemboySlots.HEAD_ACCESSORY);
    /** Fashion Critic drop only. */
    public static final RegistrySupplier<Item> WOLF_EARS = cosmetic("wolf_ears", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> OVERSIZED_HOODIE = cosmetic("oversized_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> CAT_EAR_HOODIE = cosmetic("cat_ear_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> PLEATED_SKIRT = cosmetic("pleated_skirt", FemboySlots.OUTFIT_BOTTOM);
    public static final RegistrySupplier<Item> PROGRAMMING_SOCKS = cosmetic("programming_socks", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> FISHNET_TIGHTS = cosmetic("fishnet_tights", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> UWU_CHOKER = cosmetic("uwu_choker", FemboySlots.NECK);

    // SPEC §5.3: backpacks (worn in the back slot) and charms
    public static final RegistrySupplier<Item> CANVAS_BACKPACK = backpack("canvas_backpack", 1, false);
    public static final RegistrySupplier<Item> LEATHER_BACKPACK = backpack("leather_backpack", 2, false);
    public static final RegistrySupplier<Item> NETHERITE_BACKPACK = backpack("netherite_backpack", 3, true);
    public static final RegistrySupplier<Item> SHARK_PLUSH_CHARM = register("shark_plush_charm", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> CAT_PAW_CHARM = register("cat_paw_charm", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> HEART_PIN = register("heart_pin", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> ENERGY_CAN_CHARM = register("energy_can_charm", props -> new Item(props.stacksTo(1)));

    // SPEC §5.2: Byte Energy (balance in data/femboymod/femboymod/energy_drink/*.json)
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.BLOCK);
    public static final RegistrySupplier<Block> EMPTY_ENERGY_CAN_BLOCK = BLOCKS.register("empty_energy_can", () -> new EmptyCanBlock(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "empty_energy_can")))
                    .noOcclusion().strength(0.3F).sound(SoundType.METAL).pushReaction(PushReaction.POPPED)));
    public static final RegistrySupplier<Item> EMPTY_ENERGY_CAN = register("empty_energy_can",
            props -> new BlockItem(EMPTY_ENERGY_CAN_BLOCK.get(), props.useBlockDescriptionPrefix()));
    public static final RegistrySupplier<Item> BYTE_ENERGY_PINK = energyDrink("byte_energy_pink");
    public static final RegistrySupplier<Item> BYTE_ENERGY_BLUE = energyDrink("byte_energy_blue");
    public static final RegistrySupplier<Item> BYTE_ENERGY_PURPLE = energyDrink("byte_energy_purple");
    public static final RegistrySupplier<Item> BYTE_ENERGY_RAINBOW = energyDrink("byte_energy_rainbow");

    // SPEC §5.4: Pink Creeper drops and spawn egg
    public static final RegistrySupplier<Item> GLITTER = register("glitter", props -> new Item(props));
    public static final RegistrySupplier<Item> PINK_CREEPER_SPAWN_EGG = spawnEgg("pink_creeper_spawn_egg", FemboyEntities.PINK_CREEPER_KEY);

    // Hostile meme mobs: drops and spawn eggs
    public static final RegistrySupplier<Item> GLITCH_SHARD = register("glitch_shard", props -> new Item(props));
    public static final RegistrySupplier<Item> BUG_SPAWN_EGG = spawnEgg("bug_spawn_egg", FemboyEntities.BUG_KEY);
    public static final RegistrySupplier<Item> CAFFEINATED_ZOMBIE_SPAWN_EGG = spawnEgg("caffeinated_zombie_spawn_egg", FemboyEntities.CAFFEINATED_ZOMBIE_KEY);
    public static final RegistrySupplier<Item> HISSY_CAT_SPAWN_EGG = spawnEgg("hissy_cat_spawn_egg", FemboyEntities.HISSY_CAT_KEY);

    /** Hair clip shapes (SPEC §5.1: "10 forms"); all share the hair_clip renderer. */
    public static final List<String> HAIR_CLIP_SHAPES = List.of(
            "heart", "star", "bow", "flower", "moon", "cherry", "bunny", "fish", "lightning", "butterfly");
    public static final List<RegistrySupplier<Item>> HAIR_CLIPS = HAIR_CLIP_SHAPES.stream()
            .map(shape -> cosmetic("hair_clip_" + shape, new Cosmetic(FemboySlots.HEAD_ACCESSORY,
                    Optional.of(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "hair_clip")))))
            .toList();

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.femboymod"))
            .icon(() -> new ItemStack(CAT_EARS.get()))
            .displayItems(CreativeTabContents::fill)));

    private FemboyItems() {
    }

    private static final int DRINK_STACK = 16;

    /** Block item for a block registered elsewhere (FemboyBlocks). */
    public static RegistrySupplier<Item> blockItem(String name, RegistrySupplier<Block> block) {
        return register(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix()));
    }

    private static RegistrySupplier<Item> spawnEgg(String name, ResourceKey<EntityType<?>> entity) {
        return register(name, props -> new SpawnEggItem(
                // ENTITY_DATA resolved late: on NeoForge items may be built before entity types are registered
                props.delayedComponent(DataComponents.ENTITY_DATA, context -> TypedEntityData.of(
                        context.lookupOrThrow(Registries.ENTITY_TYPE).getOrThrow(entity).value(), new CompoundTag()))));
    }

    private static RegistrySupplier<Item> energyDrink(String name) {
        return register(name, props -> new EnergyDrinkItem(props.stacksTo(DRINK_STACK)
                .component(DataComponents.CONSUMABLE, Consumables.defaultDrink().build())
                .usingConvertsTo(EMPTY_ENERGY_CAN.get())));
    }

    private static RegistrySupplier<Item> backpack(String name, int rows, boolean indestructible) {
        return register(name, props -> {
            props.stacksTo(1)
                    .component(FemboyComponents.COSMETIC.get(), new Cosmetic(FemboySlots.BACK))
                    .component(FemboyComponents.BACKPACK.get(), new BackpackSpec(rows))
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .component(FemboyComponents.CHARMS.get(), ItemContainerContents.EMPTY);
            if (indestructible) {
                // Netherite: does not burn and survives lava, cactus and explosions as a dropped item
                props.delayedComponent(DataComponents.DAMAGE_RESISTANT,
                        context -> new DamageResistant(context.getOrThrow(FemboyTags.BACKPACK_IMMUNE_TO)));
            }
            return new BackpackItem(props);
        });
    }

    private static RegistrySupplier<Item> cosmetic(String name, Identifier slot) {
        return cosmetic(name, new Cosmetic(slot));
    }

    private static RegistrySupplier<Item> cosmetic(String name, Cosmetic cosmetic) {
        return register(name, props -> new Item(props.stacksTo(1).component(FemboyComponents.COSMETIC.get(), cosmetic)));
    }

    private static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
        RegistrySupplier<Item> item = REGISTER.register(name, () -> factory.apply(new Item.Properties().setId(key)));
        TAB_ORDER.add(item);
        return item;
    }
}
