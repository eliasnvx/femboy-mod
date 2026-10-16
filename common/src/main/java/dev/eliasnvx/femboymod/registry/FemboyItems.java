package dev.eliasnvx.femboymod.registry;

import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.item.LookbookItem;
import dev.eliasnvx.femboymod.item.PhoneItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import dev.eliasnvx.femboymod.item.DyeableCosmeticItem;
import dev.eliasnvx.femboymod.item.ItemList;
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
import java.util.function.Supplier;

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
    // 0.2.0: the feet slot (over socks) and paw gloves
    public static final RegistrySupplier<Item> SNEAKERS = cosmetic("sneakers", FemboySlots.FEET);
    public static final RegistrySupplier<Item> CANVAS_HIGH_TOPS = cosmetic("canvas_high_tops", FemboySlots.FEET);
    public static final RegistrySupplier<Item> PLATFORM_SNEAKERS = cosmetic("platform_sneakers", FemboySlots.FEET);
    public static final RegistrySupplier<Item> BUNNY_SLIPPERS = cosmetic("bunny_slippers", FemboySlots.FEET);
    public static final RegistrySupplier<Item> CAT_PAW_GLOVES = cosmetic("cat_paw_gloves", FemboySlots.HANDS);
    // 0.2.0 Halloween costume (#femboymod:halloween_costume, trick-or-treat) and its candy
    public static final RegistrySupplier<Item> WITCH_HAT = cosmetic("witch_hat", FemboySlots.HEAD_ACCESSORY);
    public static final RegistrySupplier<Item> PUMPKIN_HOODIE = cosmetic("pumpkin_hoodie", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> GHOST_SHEET = cosmetic("ghost_sheet", FemboySlots.OUTFIT_TOP);
    public static final RegistrySupplier<Item> CANDY_PAIL = cosmetic("candy_pail", FemboySlots.WAIST);
    /** Eaten fast; a short sugar rush. Villagers hand it out to costumed players in the Halloween season. */
    public static final RegistrySupplier<Item> CANDY = register("candy", FemboyItems::candy);
    // 0.2.0 pet outfits for tamed cats, wolves and parrots (PetOutfits)
    public static final RegistrySupplier<Item> PET_BOW = petOutfit("pet_bow", PetOutfitItem.Slot.HEAD);
    public static final RegistrySupplier<Item> MINI_WITCH_HAT = petOutfit("mini_witch_hat", PetOutfitItem.Slot.HEAD);
    public static final RegistrySupplier<Item> BELL_COLLAR = petOutfit("bell_collar", PetOutfitItem.Slot.NECK);
    public static final RegistrySupplier<Item> PET_BANDANA = petOutfit("pet_bandana", PetOutfitItem.Slot.NECK);

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
    /** Guide book: every cosmetic, where to get it, what you've collected. */
    public static final RegistrySupplier<Item> LOOKBOOK = register("lookbook", props -> new LookbookItem(props.stacksTo(1)));
    /**
     * A printed selfie: the picture lives on the server, the item carries its id. Built per loader: Forge attaches
     * its custom item renderer to the item itself (IClientItemExtensions).
     */
    public static final RegistrySupplier<Item> PHOTO = register("photo",
            props -> dev.eliasnvx.femboymod.platform.PlatformHelper.createPhotoItem(props.stacksTo(16)));

    // SPEC v1.1: food. Nutrition like comparable vanilla food; buffs are data-driven (bubble_tea_flavor)
    public static final RegistrySupplier<Item> BUBBLE_TEA = register("bubble_tea", props -> new BubbleTeaItem(props.stacksTo(FemboyItems.DRINK_STACK)
            .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).alwaysEat().build()), () -> Items.GLASS_BOTTLE));
    public static final RegistrySupplier<Item> STRAWBERRY_MILK = register("strawberry_milk", props -> new StrawberryMilkItem(props.stacksTo(FemboyItems.DRINK_STACK)
            .food(new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).alwaysEat().build()), () -> Items.GLASS_BOTTLE));
    public static final RegistrySupplier<Item> MOCHI = register("mochi", props -> new Item(props
            .food(new FoodProperties.Builder().nutrition(3).saturationMod(0.5F).fast().build()))); // fast = 0.8 s
    public static final RegistrySupplier<Item> ONIGIRI = register("onigiri", props -> new Item(props
            .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.7F).build())));

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
    public static final RegistrySupplier<Item> SOCK_GOBLIN_SPAWN_EGG = spawnEgg("sock_goblin_spawn_egg", FemboyEntities.SOCK_GOBLIN);
    public static final RegistrySupplier<Item> RUBBER_DUCK_SPAWN_EGG = spawnEgg("rubber_duck_spawn_egg", FemboyEntities.RUBBER_DUCK);
    /** Dropped by Sock Goblins; two make a pair of Programming Socks. */
    public static final RegistrySupplier<Item> LOST_SOCK = register("lost_sock", props -> new Item(props));

    /** Hair clip shapes (SPEC §5.1: "10 forms"); all share the hair_clip renderer. */
    public static final List<String> HAIR_CLIP_SHAPES = List.of(
            "heart", "star", "bow", "flower", "moon", "cherry", "bunny", "fish", "lightning", "butterfly");
    public static final List<RegistrySupplier<Item>> HAIR_CLIPS = HAIR_CLIP_SHAPES.stream()
            .map(shape -> cosmetic("hair_clip_" + shape, new Cosmetic(FemboySlots.HEAD_ACCESSORY,
                    Optional.of(new ResourceLocation(FemboyMod.MOD_ID, "hair_clip")))))
            .toList();

    // Creative tabs (ids in api FemboyCreativeTabs), in menu order; what goes where: CreativeTabContents
    public static final RegistrySupplier<CreativeModeTab> TAB = tab(CreativeTabContents.Tab.OUTFITS, "itemGroup.femboymod",
            () -> new ItemStack(CAT_EARS.get()));
    public static final RegistrySupplier<CreativeModeTab> ROOM_TAB = tab(CreativeTabContents.Tab.ROOM, "itemGroup.femboymod.room",
            () -> new ItemStack(dev.eliasnvx.femboymod.block.FemboyBlocks.BEAN_BAG.get()));
    public static final RegistrySupplier<CreativeModeTab> SNACKS_TAB = tab(CreativeTabContents.Tab.SNACKS, "itemGroup.femboymod.snacks",
            () -> new ItemStack(BYTE_ENERGY_PINK.get()));
    public static final RegistrySupplier<CreativeModeTab> MATERIALS_TAB = tab(CreativeTabContents.Tab.MATERIALS,
            "itemGroup.femboymod.materials", () -> new ItemStack(ROSE_QUARTZ.get()));
    public static final RegistrySupplier<CreativeModeTab> MOBS_TAB = tab(CreativeTabContents.Tab.MOBS, "itemGroup.femboymod.mobs",
            () -> new ItemStack(PINK_CREEPER_SPAWN_EGG.get()));

    private FemboyItems() {
    }

    private static final int DRINK_STACK = 16;
    /**
     * 1.20.1 spawn eggs tint their texture with two colors. Ours are drawn in full color (as in 26.3),
     * so both tints are white, which leaves the texture unchanged.
     */
    private static final int NO_EGG_TINT = 0xFFFFFF;
    /** Candy's sugar rush: Speed I for 5 seconds. */
    private static final int CANDY_RUSH_TICKS = 100;
    /** How long eating a candy takes: 26.3's {@code consumeSeconds} 0.6, in ticks. */
    private static final int CANDY_EAT_TICKS = 12;

    private static RegistrySupplier<CreativeModeTab> tab(CreativeTabContents.Tab tab, String title, Supplier<ItemStack> icon) {
        return TABS.register(tab.id, () -> CreativeTabRegistry.create(builder -> builder
                .title(Component.translatable(title))
                .icon(icon)
                .displayItems((parameters, output) -> CreativeTabContents.fill(tab, parameters, output))));
    }

    /** Block item for a block registered elsewhere (FemboyBlocks). */
    public static RegistrySupplier<Item> blockItem(String name, RegistrySupplier<Block> block) {
        return register(name, props -> new BlockItem(block.get(), props));
    }

    private static RegistrySupplier<Item> spawnEgg(String name, RegistrySupplier<? extends EntityType<? extends Mob>> entity) {
        // Architectury resolves the entity type late: on NeoForge items may be built before entity types are registered
        return register(name, props -> new ArchitecturySpawnEggItem(entity, NO_EGG_TINT, NO_EGG_TINT, props));
    }

    /**
     * Candy: always edible, then the sugar rush. 1.20.1's food only eats in 1.6 s or 0.8 s ({@code fast}), so the
     * item sets its own eating time.
     */
    private static Item candy(Item.Properties props) {
        FoodProperties food = new FoodProperties.Builder().nutrition(2).saturationMod(0.1F).alwaysEat()
                .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CANDY_RUSH_TICKS, 0), 1.0F).build();
        return new Item(props.food(food)) {
            @Override
            public int getUseDuration(ItemStack stack) {
                return CANDY_EAT_TICKS;
            }
        };
    }

    private static RegistrySupplier<Item> petOutfit(String name, PetOutfitItem.Slot slot) {
        return register(name, props -> new PetOutfitItem(props, slot));
    }

    private static RegistrySupplier<Item> energyDrink(String name) {
        return register(name, props -> new EnergyDrinkItem(props.stacksTo(DRINK_STACK), EMPTY_ENERGY_CAN));
    }

    private static RegistrySupplier<Item> backpack(String name, int rows, boolean indestructible) {
        return register(name, props -> {
            props.stacksTo(1);
            if (indestructible) {
                // Netherite: does not burn and survives lava, cactus and explosions as a dropped item.
                // 1.20.1 has no damage_resistant component: fireResistant covers fire and lava,
                // the rest of #femboymod:backpack_immune_to goes through isDamageResistant.
                props.fireResistant();
            }
            BackpackItem item = new BackpackItem(props);
            // 1.20.1 has no default components: every backpack stack falls back to these values
            FemboyComponents.COSMETIC.setDefault(item, new Cosmetic(FemboySlots.BACK));
            FemboyComponents.BACKPACK.setDefault(item, new BackpackSpec(rows));
            FemboyComponents.CONTENTS.setDefault(item, ItemList.EMPTY);
            FemboyComponents.CHARMS.setDefault(item, ItemList.EMPTY);
            return item;
        });
    }

    /** A cosmetic vanilla can dye (1.21.1: listed in #femboymod:dyeable_cosmetics; see DyeableCosmeticItem). */
    private static RegistrySupplier<Item> cosmetic(String name, ResourceLocation slot) {
        return cosmetic(name, new Cosmetic(slot), DyeableCosmeticItem::new);
    }

    private static RegistrySupplier<Item> cosmetic(String name, Cosmetic cosmetic) {
        return cosmetic(name, cosmetic, Item::new);
    }

    private static RegistrySupplier<Item> cosmetic(String name, Cosmetic cosmetic, Function<Item.Properties, Item> factory) {
        return register(name, props -> {
            Item item = factory.apply(props.stacksTo(1));
            FemboyComponents.COSMETIC.setDefault(item, cosmetic); // 1.21.1: default component
            return item;
        });
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
