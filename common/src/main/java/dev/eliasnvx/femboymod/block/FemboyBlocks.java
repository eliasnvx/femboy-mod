package dev.eliasnvx.femboymod.block;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Set;
import java.util.function.Function;

/** Blocks and the Thrifter profession. */
public final class FemboyBlocks {

    public static final DeferredRegister<Block> BLOCKS = FemboyItems.BLOCKS;
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(FemboyMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.VILLAGER_PROFESSION);

    public static final RegistrySupplier<Block> CLOTHING_RACK = block("clothing_rack", ClothingRackBlock::new,
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final RegistrySupplier<Block> WARDROBE = block("wardrobe", WardrobeBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD));
    public static final RegistrySupplier<Block> MIRROR = block("mirror", MirrorBlock::new,
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.GLASS).noOcclusion());
    public static final RegistrySupplier<Block> GILDED_MIRROR = block("gilded_mirror", MirrorBlock::new,
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.GLASS).noOcclusion());
    public static final RegistrySupplier<Block> SHARK_PLUSH = plush("shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.SPLASH);
    public static final RegistrySupplier<Block> PINK_SHARK_PLUSH = plush("pink_shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES);
    public static final RegistrySupplier<Block> GRAY_SHARK_PLUSH = plush("gray_shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.SPLASH);
    public static final RegistrySupplier<Block> PURPLE_SHARK_PLUSH = plush("purple_shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.WITCH);
    public static final RegistrySupplier<Block> PASTEL_SHARK_PLUSH = plush("pastel_shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES);
    public static final RegistrySupplier<Block> RAINBOW_SHARK_PLUSH = plush("rainbow_shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5),
            net.minecraft.core.particles.ParticleTypes.FIREWORK);
    public static final RegistrySupplier<Block> CAT_PLUSH = plush("cat_plush", Block.box(3.5, 0.0, 4.5, 14.0, 13.5, 11.5),
            net.minecraft.core.particles.ParticleTypes.HEART);
    public static final RegistrySupplier<Block> CREEPER_PLUSH = plush("creeper_plush", Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0),
            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);
    public static final RegistrySupplier<Block> PINK_CREEPER_PLUSH = plush("pink_creeper_plush", Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0),
            net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES);
    public static final RegistrySupplier<Block> ENDERMAN_PLUSH = plush("enderman_plush", Block.box(4.0, 0.0, 2.5, 12.0, 12.7, 11.6),
            net.minecraft.core.particles.ParticleTypes.PORTAL);
    public static final RegistrySupplier<Block> DRAGON_PLUSH = plush("dragon_plush", Block.box(2.0, 0.0, 2.5, 14.0, 12.8, 14.8),
            net.minecraft.core.particles.ParticleTypes.PORTAL);
    public static final RegistrySupplier<Block> AXOLOTL_PLUSH = plush("axolotl_plush", Block.box(1.5, 0.0, 2.5, 14.5, 6.0, 15.2),
            net.minecraft.core.particles.ParticleTypes.SPLASH);
    public static final RegistrySupplier<Block> BEE_PLUSH = plush("bee_plush", Block.box(2.0, 0.0, 2.0, 14.0, 9.0, 14.0),
            net.minecraft.core.particles.ParticleTypes.FALLING_NECTAR);
    public static final RegistrySupplier<Block> FROG_PLUSH = plush("frog_plush", Block.box(2.6, 0.0, 2.8, 13.4, 9.0, 11.6),
            net.minecraft.core.particles.ParticleTypes.SPLASH);
    public static final RegistrySupplier<Block> BUNNY_PLUSH = plush("bunny_plush", Block.box(1.6, 0.0, 4.5, 14.4, 12.1, 13.8),
            net.minecraft.core.particles.ParticleTypes.HEART);
    public static final RegistrySupplier<Block> FOX_PLUSH = plush("fox_plush", Block.box(3.8, 0.0, 1.6, 14.4, 7.8, 12.2),
            net.minecraft.core.particles.ParticleTypes.GLOW);
    public static final RegistrySupplier<Block> CAPYBARA_PLUSH = plush("capybara_plush", Block.box(3.6, 0.0, 1.5, 12.4, 13.4, 12.2),
            net.minecraft.core.particles.ParticleTypes.BUBBLE_POP);
    public static final RegistrySupplier<Block> PENGUIN_PLUSH = plush("penguin_plush", Block.box(2.0, 0.0, 3.2, 14.0, 13.0, 12.0),
            net.minecraft.core.particles.ParticleTypes.SNOWFLAKE);
    public static final RegistrySupplier<Block> BEAR_PLUSH = plush("bear_plush", Block.box(3.2, 0.0, 3.2, 12.8, 13.4, 13.2),
            net.minecraft.core.particles.ParticleTypes.HEART);
    public static final RegistrySupplier<Block> SEAL_PLUSH = plush("seal_plush", Block.box(1.2, 0.0, 2.8, 14.8, 7.6, 15.4),
            net.minecraft.core.particles.ParticleTypes.SPLASH);
    public static final RegistrySupplier<Block> RED_PANDA_PLUSH = plush("red_panda_plush", Block.box(3.4, 0.0, 3.2, 14.8, 13.2, 13.2),
            net.minecraft.core.particles.ParticleTypes.GLOW);
    public static final RegistrySupplier<Block> RUBBER_DUCK = plushBlock("rubber_duck",
            props -> new RubberDuckBlock(props, Block.box(4.0, 0.0, 3.5, 12.0, 11.0, 14.0)),
            BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistrySupplier<Block> PLUSH_SHELF = block("plush_shelf", PlushShelfBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());

    // Ores (rose quartz in mountains and caves, glitter in flower biomes). Generation: data/femboymod/worldgen.
    public static final RegistrySupplier<Block> ROSE_QUARTZ_ORE = block("rose_quartz_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 5), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .instrument(NoteBlockInstrument.BASEDRUM));
    public static final RegistrySupplier<Block> DEEPSLATE_ROSE_QUARTZ_ORE = block("deepslate_rose_quartz_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 5), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM));
    public static final RegistrySupplier<Block> GLITTER_ORE = block("glitter_ore",
            props -> new DropExperienceBlock(UniformInt.of(1, 3), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .instrument(NoteBlockInstrument.BASEDRUM));
    public static final RegistrySupplier<Block> ROSE_QUARTZ_BLOCK = block("rose_quartz_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST));

    // Rose quartz geodes: budding blocks grow buds into clusters, like amethyst
    public static final RegistrySupplier<Block> BUDDING_ROSE_QUARTZ = block("budding_rose_quartz", BuddingRoseQuartzBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).randomTicks().strength(1.5F).sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.POPPED));
    public static final RegistrySupplier<Block> ROSE_QUARTZ_CLUSTER = cluster("rose_quartz_cluster", 7.0F, 3.0F, SoundType.AMETHYST_CLUSTER, 5);
    public static final RegistrySupplier<Block> LARGE_ROSE_QUARTZ_BUD = cluster("large_rose_quartz_bud", 5.0F, 3.0F, SoundType.LARGE_AMETHYST_BUD, 4);
    public static final RegistrySupplier<Block> MEDIUM_ROSE_QUARTZ_BUD = cluster("medium_rose_quartz_bud", 4.0F, 3.0F, SoundType.MEDIUM_AMETHYST_BUD, 2);
    public static final RegistrySupplier<Block> SMALL_ROSE_QUARTZ_BUD = cluster("small_rose_quartz_bud", 3.0F, 4.0F, SoundType.SMALL_AMETHYST_BUD, 1);

    // Moonstone: deep ore that glows faintly, night jewelry, the pearl colorway and a night light
    public static final RegistrySupplier<Block> DEEPSLATE_MOONSTONE_ORE = block("deepslate_moonstone_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).lightLevel(state -> 3));
    public static final RegistrySupplier<Block> MOONSTONE_LAMP = block("moonstone_lamp", MoonstoneLampBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8F).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(state -> state.getValue(MoonstoneLampBlock.LIT) ? MoonstoneLampBlock.LIGHT : 0));

    // Neon quartz (Nether): glowing ore, neon signs, the cyber visor and the neon colorway
    public static final RegistrySupplier<Block> NEON_QUARTZ_ORE = block("neon_quartz_ore",
            props -> new DropExperienceBlock(UniformInt.of(2, 5), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHER_ORE).instrument(NoteBlockInstrument.BASEDRUM).lightLevel(state -> 4));
    public static final RegistrySupplier<Block> NEON_SIGN = block("neon_sign", NeonSignBlock::new,
            BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> NeonSignBlock.LIGHT));

    // Gamer corner furniture
    public static final RegistrySupplier<Block> GAMER_CHAIR = block("gamer_chair", GamerChairBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistrySupplier<Block> GAMER_MONITOR = block("gamer_monitor", GamerMonitorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(GamerMonitorBlock.LIT) ? GamerMonitorBlock.LIGHT : 0));
    public static final RegistrySupplier<Block> GAMER_KEYBOARD = block("gamer_keyboard", GamerKeyboardBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.STONE).noOcclusion());
    public static final RegistrySupplier<Block> GAMER_DESK = block("gamer_desk", GamerDeskBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final RegistrySupplier<Block> GAMING_PC = block("gaming_pc",
            props -> new ToggleableDecorBlock(props, Block.box(4.0, 0.0, 1.5, 12.0, 14.5, 14.5)),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(ToggleableDecorBlock.LIT) ? 5 : 0));
    public static final RegistrySupplier<Block> RING_LIGHT = block("ring_light",
            props -> new ToggleableDecorBlock(props, Block.box(3.5, 0.0, 3.5, 12.5, 16.0, 12.5)),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(ToggleableDecorBlock.LIT) ? 13 : 0));
    public static final RegistrySupplier<Block> DESK_MIC = block("desk_mic",
            props -> new SimpleDecorBlock(props, Block.box(5.0, 0.0, 4.5, 11.0, 13.0, 11.0)),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6F).sound(SoundType.METAL).noOcclusion());
    public static final RegistrySupplier<Block> TERMINAL = block("terminal", TerminalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistrySupplier<Block> VIBE_SCANNER = block("vibe_scanner", dev.eliasnvx.femboymod.vibe.VibeScannerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> dev.eliasnvx.femboymod.vibe.VibeScannerBlock.LIGHT));
    public static final RegistrySupplier<Block> LED_STRIP = block("led_strip", LedStripBlock::new,
            BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion().noCollision()
                    .lightLevel(state -> LedStripBlock.LIGHT));

    // Cozy interior (0.2.0)
    public static final RegistrySupplier<Block> BEAN_BAG = block("bean_bag", BeanBagBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistrySupplier<Block> FLUFFY_RUG = block("fluffy_rug", FluffyRugBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1F).sound(SoundType.WOOL).noOcclusion().ignitedByLava());
    public static final RegistrySupplier<Block> FAIRY_LIGHTS = block("fairy_lights", FairyLightsBlock::new,
            BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GLASS).noOcclusion().noCollision()
                    .lightLevel(state -> FairyLightsBlock.LIGHT));
    public static final RegistrySupplier<Block> LAVA_LAMP = block("lava_lamp", LavaLampBlock::new,
            BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(state -> state.getValue(LavaLampBlock.LIT) ? LavaLampBlock.LIGHT : 0));
    public static final RegistrySupplier<Block> PET_BED = block("pet_bed", PetBedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistrySupplier<Block> LAUNDRY_BASKET = block("laundry_basket", LaundryBasketBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.BAMBOO_WOOD).noOcclusion());
    public static final RegistrySupplier<Block> MINI_FRIDGE = block("mini_fridge", MiniFridgeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistrySupplier<Block> VANITY_TABLE = block("vanity_table", VanityTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion());

    // Plants (0.3.0): the Debug Flytrap (swamps), catnip (plains, meadows) and Potted Grass. Generation: data/femboymod/worldgen.
    public static final RegistrySupplier<Block> DEBUG_FLYTRAP = block("debug_flytrap", DebugFlytrapBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().randomTicks().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.POPPED));
    public static final RegistrySupplier<Block> POTTED_DEBUG_FLYTRAP = blockWithoutItem("potted_debug_flytrap",
            props -> new PottedDebugFlytrapBlock(DEBUG_FLYTRAP.get(), props), flowerPot().randomTicks());
    public static final RegistrySupplier<Block> CATNIP = block("catnip", CatnipBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().randomTicks().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED));
    public static final RegistrySupplier<Block> POTTED_CATNIP = blockWithoutItem("potted_catnip",
            props -> new FlowerPotBlock(CATNIP.get(), props), flowerPot());
    public static final RegistrySupplier<Block> POTTED_GRASS = blockWithoutItem("potted_grass",
            props -> new PottedGrassBlock(Blocks.SHORT_GRASS, props), flowerPot());
    public static final RegistrySupplier<Block> STRAWBERRY_BUSH = blockWithoutItem("strawberry_bush", StrawberryBushBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().randomTicks()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.POPPED));
    public static final RegistrySupplier<Block> SAKURA_BONSAI = block("sakura_bonsai",
            props -> new SimpleDecorBlock(props, Block.box(2.5, 0.0, 3.5, 13.5, 15.0, 12.5)),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final RegistrySupplier<Block> CAT_SUCCULENT = block("cat_succulent",
            props -> new SimpleDecorBlock(props, Block.box(4.0, 0.0, 4.0, 12.0, 10.5, 12.0)),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.DECORATED_POT).noOcclusion());

    public static final RegistrySupplier<BlockEntityType<ClothingRackBlockEntity>> CLOTHING_RACK_ENTITY = BLOCK_ENTITIES.register(
            "clothing_rack", () -> new BlockEntityType<>(ClothingRackBlockEntity::new, Set.of(CLOTHING_RACK.get())));
    public static final RegistrySupplier<BlockEntityType<MirrorBlockEntity>> MIRROR_ENTITY = BLOCK_ENTITIES.register(
            "mirror", () -> new BlockEntityType<>(MirrorBlockEntity::new, Set.of(MIRROR.get(), GILDED_MIRROR.get(), VANITY_TABLE.get())));
    public static final RegistrySupplier<BlockEntityType<WardrobeBlockEntity>> WARDROBE_ENTITY = BLOCK_ENTITIES.register(
            "wardrobe", () -> new BlockEntityType<>(WardrobeBlockEntity::new, Set.of(WARDROBE.get())));

    public static final RegistrySupplier<BlockEntityType<LaundryBasketBlockEntity>> LAUNDRY_BASKET_ENTITY = BLOCK_ENTITIES.register(
            "laundry_basket", () -> new BlockEntityType<>(LaundryBasketBlockEntity::new, Set.of(LAUNDRY_BASKET.get())));
    public static final RegistrySupplier<BlockEntityType<MiniFridgeBlockEntity>> MINI_FRIDGE_ENTITY = BLOCK_ENTITIES.register(
            "mini_fridge", () -> new BlockEntityType<>(MiniFridgeBlockEntity::new, Set.of(MINI_FRIDGE.get())));
    public static final RegistrySupplier<BlockEntityType<PlushBlockEntity>> PLUSH_ENTITY = BLOCK_ENTITIES.register(
            "plush", () -> new BlockEntityType<>(PlushBlockEntity::new,
                    Set.of(SHARK_PLUSH.get(), PINK_SHARK_PLUSH.get(), GRAY_SHARK_PLUSH.get(),
                            PURPLE_SHARK_PLUSH.get(), PASTEL_SHARK_PLUSH.get(), RAINBOW_SHARK_PLUSH.get(),
                            CAT_PLUSH.get(), CREEPER_PLUSH.get(), PINK_CREEPER_PLUSH.get(),
                            ENDERMAN_PLUSH.get(), DRAGON_PLUSH.get(), AXOLOTL_PLUSH.get(),
                            BEE_PLUSH.get(), FROG_PLUSH.get(), BUNNY_PLUSH.get(), FOX_PLUSH.get(),
                            CAPYBARA_PLUSH.get(), PENGUIN_PLUSH.get(), BEAR_PLUSH.get(),
                            SEAL_PLUSH.get(), RED_PANDA_PLUSH.get(),
                            RUBBER_DUCK.get())));
    public static final RegistrySupplier<BlockEntityType<PlushShelfBlockEntity>> PLUSH_SHELF_ENTITY = BLOCK_ENTITIES.register(
            "plush_shelf", () -> new BlockEntityType<>(PlushShelfBlockEntity::new, Set.of(PLUSH_SHELF.get())));

    /** Laundry baskets are points of interest so Sock Goblins find them (registered per loader with the Thrifter's). */
    public static final ResourceKey<PoiType> LAUNDRY_BASKET_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("laundry_basket"));

    /** The clothing rack is the Thrifter's job site (POI registered per loader, see PlatformHelper#registerPoi). */
    public static final ResourceKey<PoiType> THRIFTER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("thrifter"));
    private static final int MAX_LEVEL = 5;

    public static final RegistrySupplier<VillagerProfession> THRIFTER = PROFESSIONS.register("thrifter", () -> {
        Int2ObjectMap.Entry<ResourceKey<TradeSet>>[] levels = new Int2ObjectMap.Entry[MAX_LEVEL];
        for (int level = 1; level <= MAX_LEVEL; level++) {
            levels[level - 1] = Int2ObjectMap.entry(level, ResourceKey.create(Registries.TRADE_SET, id("thrifter/level_" + level)));
        }
        return new VillagerProfession(Component.translatable("entity.femboymod.villager.thrifter"),
                poi -> poi.is(THRIFTER_POI), poi -> poi.is(THRIFTER_POI),
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LEATHERWORKER, Int2ObjectMap.ofEntries(levels));
    });

    private FemboyBlocks() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    private static RegistrySupplier<Block> block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        RegistrySupplier<Block> block = BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, id(name)))));
        FemboyItems.blockItem(name, block);
        return block;
    }

    /** Potted plants: no item of their own (made by putting the plant into a flower pot), like vanilla's. */
    private static RegistrySupplier<Block> blockWithoutItem(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                            BlockBehaviour.Properties props) {
        return BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, id(name)))));
    }

    /** Vanilla's flower pot properties (private in {@code Blocks}). */
    private static BlockBehaviour.Properties flowerPot() {
        return BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED);
    }

    /** Rose quartz bud/cluster: amethyst-style crystal on any face (height and side inset in pixels). */
    private static RegistrySupplier<Block> cluster(String name, float height, float inset, SoundType sound, int light) {
        return block(name, props -> new AmethystClusterBlock(height, inset, props),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).forceSolidOn().noOcclusion().sound(sound).strength(1.5F)
                        .lightLevel(state -> light).pushReaction(PushReaction.POPPED));
    }

    /** Shapes match tools/plush/make_plushies.py (face pointing north). */
    private static RegistrySupplier<Block> plush(String name, VoxelShape northShape) {
        return plush(name, northShape, null);
    }

    private static RegistrySupplier<Block> plushBlock(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        RegistrySupplier<Block> block = BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, id(name)))));
        FemboyItems.plushBlockItem(name, block);
        return block;
    }

    private static RegistrySupplier<Block> plush(String name, VoxelShape northShape,
                                                 @org.jetbrains.annotations.Nullable net.minecraft.core.particles.SimpleParticleType extraParticle) {
        return plushBlock(name, props -> new PlushBlock(props, northShape, extraParticle),
                BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    }

    /** The cozy interior pieces (0.2.0) and the plants (0.3.0): their items get a how-to tooltip ({@code tooltip.femboymod.<name>}). */
    public static boolean hasHowTo(Block block) {
        return block == BEAN_BAG.get() || block == FLUFFY_RUG.get() || block == FAIRY_LIGHTS.get() || block == LAVA_LAMP.get()
                || block == PET_BED.get() || block == LAUNDRY_BASKET.get() || block == MINI_FRIDGE.get() || block == VANITY_TABLE.get()
                || block == PLUSH_SHELF.get() || block == DEBUG_FLYTRAP.get() || block == CATNIP.get();
    }

    /** Called from FemboyMod.init after the block register. */
    public static void init() {
        BLOCK_ENTITIES.register();
        PROFESSIONS.register();
    }
}
