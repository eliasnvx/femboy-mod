package dev.eliasnvx.femboymod.registry;

import dev.eliasnvx.femboymod.item.PhoneItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import dev.eliasnvx.femboymod.food.StrawberryMilkItem;
import dev.eliasnvx.femboymod.food.BubbleTeaItem;
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
import dev.architectury.core.item.ArchitecturySpawnEggItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import dev.eliasnvx.femboymod.entity.FemboyEntities;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import dev.eliasnvx.femboymod.backpack.BackpackItem;
import dev.eliasnvx.femboymod.backpack.BackpackSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    public static final RegistrySupplier<Item> STRIPED_MITTENS = cosmetic("striped_mittens", FemboySlots.HANDS);
    public static final RegistrySupplier<Item> OVERSIZED_HOODIE = cosmetic("oversized_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> CAT_EAR_HOODIE = cosmetic("cat_ear_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> PLEATED_SKIRT = cosmetic("pleated_skirt", FemboySlots.OUTFIT_BOTTOM);
    public static final RegistrySupplier<Item> PROGRAMMING_SOCKS = cosmetic("programming_socks", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> FISHNET_TIGHTS = cosmetic("fishnet_tights", FemboySlots.LEGS_OVERLAY);
    public static final RegistrySupplier<Item> UWU_CHOKER = cosmetic("uwu_choker", FemboySlots.NECK);
    // SPEC v1.1 clothing
    public static final RegistrySupplier<Item> CAT_EAR_HEADPHONES = cosmetic("cat_ear_headphones", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> HEART_GLASSES = cosmetic("heart_glasses", FemboySlots.FACE);
    public static final RegistrySupplier<Item> ARM_WARMERS = cosmetic("arm_warmers", FemboySlots.HANDS);
    public static final RegistrySupplier<Item> NAIL_POLISH = cosmetic("nail_polish", FemboySlots.HANDS);
    public static final RegistrySupplier<Item> CROP_SWEATER = cosmetic("crop_sweater", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> BELT_CHAINS = cosmetic("belt_chains", FemboySlots.WAIST);
    /** Rose quartz jewelry (crafted from the ore's crystals). */
    public static final RegistrySupplier<Item> ROSE_QUARTZ_EARRINGS = cosmetic("rose_quartz_earrings", FemboySlots.FACE);
    public static final RegistrySupplier<Item> ROSE_QUARTZ_BRACELET = cosmetic("rose_quartz_bracelet", FemboySlots.HANDS);
    public static final RegistrySupplier<Item> MOONSTONE_PENDANT = cosmetic("moonstone_pendant", FemboySlots.NECK);
    public static final RegistrySupplier<Item> CYBER_VISOR = cosmetic("cyber_visor", FemboySlots.FACE);
    /** Chunky black sunglasses; the Fashion Critic can't argue with them (#femboymod:critic_approved). */
    public static final RegistrySupplier<Item> DARK_SHADES = cosmetic("dark_shades", FemboySlots.FACE);

    // SPEC §5.3: backpacks (worn in the back slot) and charms
    public static final RegistrySupplier<Item> CANVAS_BACKPACK = backpack("canvas_backpack", 1, false);
    public static final RegistrySupplier<Item> LEATHER_BACKPACK = backpack("leather_backpack", 2, false);
    public static final RegistrySupplier<Item> NETHERITE_BACKPACK = backpack("netherite_backpack", 3, true);
    public static final RegistrySupplier<Item> SHARK_PLUSH_CHARM = register("shark_plush_charm", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> CAT_PAW_CHARM = register("cat_paw_charm", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> HEART_PIN = register("heart_pin", props -> new Item(props.stacksTo(1)));
    public static final RegistrySupplier<Item> ENERGY_CAN_CHARM = register("energy_can_charm", props -> new Item(props.stacksTo(1)));
    /** Pride flag badge (SPEC v1.1): a charm whose flag is its colorway pattern. */
    public static final RegistrySupplier<Item> PRIDE_BADGE = register("pride_badge", props -> new Item(props.stacksTo(1)));

    // SPEC §5.2: Byte Energy (balance in data/femboymod/femboymod/energy_drink/*.json)
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.BLOCK);
    public static final RegistrySupplier<Block> EMPTY_ENERGY_CAN_BLOCK = BLOCKS.register("empty_energy_can", () -> new EmptyCanBlock(
            // DESTROY: a piston pops the can off and drops it (26.3: PushReaction.POPPED)
            BlockBehaviour.Properties.of()
                    .noOcclusion().strength(0.3F).sound(SoundType.METAL).pushReaction(PushReaction.DESTROY)));
    public static final RegistrySupplier<Item> EMPTY_ENERGY_CAN = register("empty_energy_can",
            props -> new BlockItem(EMPTY_ENERGY_CAN_BLOCK.get(), props));
    public static final RegistrySupplier<Item> BYTE_ENERGY_PINK = energyDrink("byte_energy_pink");
    public static final RegistrySupplier<Item> BYTE_ENERGY_BLUE = energyDrink("byte_energy_blue");
    public static final RegistrySupplier<Item> BYTE_ENERGY_PURPLE = energyDrink("byte_energy_purple");
    public static final RegistrySupplier<Item> BYTE_ENERGY_RAINBOW = energyDrink("byte_energy_rainbow");

    /** Photo Mode (SPEC v1.1): selfie with a frame and watermark. */
    public static final RegistrySupplier<Item> PHONE = register("phone", props -> new PhoneItem(props.stacksTo(1)));

    // SPEC v1.1: food. Nutrition like comparable vanilla food; buffs are data-driven (bubble_tea_flavor)
    public static final RegistrySupplier<Item> BUBBLE_TEA = register("bubble_tea", props -> new BubbleTeaItem(props.stacksTo(FemboyItems.DRINK_STACK)
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).alwaysEdible()
                    .usingConvertsTo(Items.GLASS_BOTTLE).build())));
    public static final RegistrySupplier<Item> STRAWBERRY_MILK = register("strawberry_milk", props -> new StrawberryMilkItem(props.stacksTo(FemboyItems.DRINK_STACK)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).alwaysEdible()
                    .usingConvertsTo(Items.GLASS_BOTTLE).build())));
    public static final RegistrySupplier<Item> MOCHI = register("mochi", props -> new Item(props
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5F).fast().build()))); // fast = 0.8 s
    public static final RegistrySupplier<Item> ONIGIRI = register("onigiri", props -> new Item(props
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build())));

    // SPEC §5.4: Pink Creeper drops and spawn egg
    public static final RegistrySupplier<Item> GLITTER = register("glitter", props -> new Item(props));
    /** Bought with Style Points (Outfit screen), spent at the Thrifter; tradeable between players. */
    public static final RegistrySupplier<Item> STYLE_COUPON = register("style_coupon", props -> new Item(props));
    /** Deep moonstone: night jewelry, the pearl colorway, the moonstone lamp. */
    public static final RegistrySupplier<Item> MOONSTONE = register("moonstone", props -> new Item(props));
    /** Nether neon quartz: neon signs, the cyber visor, the neon colorway. */
    public static final RegistrySupplier<Item> NEON_QUARTZ = register("neon_quartz", props -> new Item(props));
    /** Mined from rose quartz ore; jewelry, furniture and the rose quartz block. */
    public static final RegistrySupplier<Item> ROSE_QUARTZ = register("rose_quartz", props -> new Item(props));
    public static final RegistrySupplier<Item> PINK_CREEPER_SPAWN_EGG = spawnEgg("pink_creeper_spawn_egg", FemboyEntities.PINK_CREEPER);

    // Hostile meme mobs: drops and spawn eggs
    public static final RegistrySupplier<Item> GLITCH_SHARD = register("glitch_shard", props -> new Item(props));
    public static final RegistrySupplier<Item> BUG_SPAWN_EGG = spawnEgg("bug_spawn_egg", FemboyEntities.BUG);
    public static final RegistrySupplier<Item> CAFFEINATED_ZOMBIE_SPAWN_EGG = spawnEgg("caffeinated_zombie_spawn_egg", FemboyEntities.CAFFEINATED_ZOMBIE);
    public static final RegistrySupplier<Item> HISSY_CAT_SPAWN_EGG = spawnEgg("hissy_cat_spawn_egg", FemboyEntities.HISSY_CAT);
    public static final RegistrySupplier<Item> STRAY_CAT_SPAWN_EGG = spawnEgg("stray_cat_spawn_egg", FemboyEntities.STRAY_CAT);
    public static final RegistrySupplier<Item> COSPLAYER_SPAWN_EGG = spawnEgg("cosplayer_spawn_egg", FemboyEntities.COSPLAYER);
    public static final RegistrySupplier<Item> FASHION_CRITIC_SPAWN_EGG = spawnEgg("fashion_critic_spawn_egg", FemboyEntities.FASHION_CRITIC);

    /** Hair clip shapes (SPEC §5.1: "10 forms"); all share the hair_clip renderer. */
    public static final List<String> HAIR_CLIP_SHAPES = List.of(
            "heart", "star", "bow", "flower", "moon", "cherry", "bunny", "fish", "lightning", "butterfly");
    public static final List<RegistrySupplier<Item>> HAIR_CLIPS = HAIR_CLIP_SHAPES.stream()
            .map(shape -> cosmetic("hair_clip_" + shape, new Cosmetic(FemboySlots.HEAD_ACCESSORY,
                    Optional.of(new ResourceLocation(FemboyMod.MOD_ID, "hair_clip")))))
            .toList();

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.femboymod"))
            .icon(() -> new ItemStack(CAT_EARS.get()))
            .displayItems(CreativeTabContents::fill)));

    private FemboyItems() {
    }

    private static final int DRINK_STACK = 16;
    /**
     * 1.21.1 spawn eggs tint their texture with two colors. Ours are drawn in full color (as in 26.3),
     * so both tints are white, which leaves the texture unchanged.
     */
    private static final int NO_EGG_TINT = 0xFFFFFF;

    /** Block item for a block registered elsewhere (FemboyBlocks). */
    public static RegistrySupplier<Item> blockItem(String name, RegistrySupplier<Block> block) {
        return register(name, props -> new BlockItem(block.get(), props));
    }

    private static RegistrySupplier<Item> spawnEgg(String name, RegistrySupplier<? extends EntityType<? extends Mob>> entity) {
        // Architectury resolves the entity type late: on NeoForge items may be built before entity types are registered
        return register(name, props -> new ArchitecturySpawnEggItem(entity, NO_EGG_TINT, NO_EGG_TINT, props));
    }

    private static RegistrySupplier<Item> energyDrink(String name) {
        return register(name, props -> new EnergyDrinkItem(props.stacksTo(DRINK_STACK), EMPTY_ENERGY_CAN));
    }

    private static RegistrySupplier<Item> backpack(String name, int rows, boolean indestructible) {
        return register(name, props -> {
            props.stacksTo(1)
                    .component(FemboyComponents.COSMETIC.get(), new Cosmetic(FemboySlots.BACK))
                    .component(FemboyComponents.BACKPACK.get(), new BackpackSpec(rows))
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .component(FemboyComponents.CHARMS.get(), ItemContainerContents.EMPTY);
            if (indestructible) {
                // Netherite: does not burn and survives lava, cactus and explosions as a dropped item.
                // 1.21.1 has no damage_resistant component: fire_resistant covers fire and lava,
                // the rest of #femboymod:backpack_immune_to goes through isDamageResistant.
                props.fireResistant();
            }
            return new BackpackItem(props);
        });
    }

    private static RegistrySupplier<Item> cosmetic(String name, ResourceLocation slot) {
        return cosmetic(name, new Cosmetic(slot));
    }

    private static RegistrySupplier<Item> cosmetic(String name, Cosmetic cosmetic) {
        return register(name, props -> new Item(props.stacksTo(1).component(FemboyComponents.COSMETIC.get(), cosmetic)));
    }

    private static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> factory) {
        RegistrySupplier<Item> item = REGISTER.register(name, () -> factory.apply(new Item.Properties()));
        TAB_ORDER.add(item);
        return item;
    }

    /**
     * Whether a dropped {@code stack} ignores {@code source}: stands in for 26.3's {@code damage_resistant}
     * component on the netherite backpack (tag {@link FemboyTags#BACKPACK_IMMUNE_TO}).
     */
    public static boolean isDamageResistant(ItemStack stack, DamageSource source) {
        return stack.is(NETHERITE_BACKPACK.get()) && source.is(FemboyTags.BACKPACK_IMMUNE_TO);
    }
}
