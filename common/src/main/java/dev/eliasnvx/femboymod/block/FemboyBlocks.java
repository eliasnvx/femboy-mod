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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Set;
import java.util.function.Function;

/** Blocks of SPEC §5.6 and the Thrifter profession (§5.5). */
public final class FemboyBlocks {

    public static final DeferredRegister<Block> BLOCKS = FemboyItems.BLOCKS;
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(FemboyMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(FemboyMod.MOD_ID, Registries.VILLAGER_PROFESSION);

    public static final RegistrySupplier<Block> CLOTHING_RACK = block("clothing_rack", ClothingRackBlock::new,
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final RegistrySupplier<Block> WARDROBE = block("wardrobe", WardrobeBlock::new,
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD));
    public static final RegistrySupplier<Block> SHARK_PLUSH = plush("shark_plush", Block.box(2.5, 0.0, 2.5, 13.5, 8.0, 15.5));
    public static final RegistrySupplier<Block> CAT_PLUSH = plush("cat_plush", Block.box(3.5, 0.0, 4.5, 14.0, 13.5, 11.5));
    public static final RegistrySupplier<Block> CREEPER_PLUSH = plush("creeper_plush", Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0));
    public static final RegistrySupplier<Block> RUBBER_DUCK = block("rubber_duck",
            props -> new RubberDuckBlock(props, Block.box(4.0, 0.0, 3.5, 12.0, 11.0, 14.0)),
            BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noOcclusion());

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

    // Gamer corner furniture (SPEC v1.1)
    public static final RegistrySupplier<Block> GAMER_CHAIR = block("gamer_chair", GamerChairBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistrySupplier<Block> GAMER_MONITOR = block("gamer_monitor", GamerMonitorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(GamerMonitorBlock.LIT) ? GamerMonitorBlock.LIGHT : 0));
    public static final RegistrySupplier<Block> GAMER_KEYBOARD = block("gamer_keyboard", GamerKeyboardBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.STONE).noOcclusion());
    public static final RegistrySupplier<Block> TERMINAL = block("terminal", TerminalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistrySupplier<Block> VIBE_SCANNER = block("vibe_scanner", dev.eliasnvx.femboymod.vibe.VibeScannerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> dev.eliasnvx.femboymod.vibe.VibeScannerBlock.LIGHT));
    public static final RegistrySupplier<Block> LED_STRIP = block("led_strip", LedStripBlock::new,
            BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion().noCollision()
                    .lightLevel(state -> LedStripBlock.LIGHT));

    public static final RegistrySupplier<BlockEntityType<ClothingRackBlockEntity>> CLOTHING_RACK_ENTITY = BLOCK_ENTITIES.register(
            "clothing_rack", () -> new BlockEntityType<>(ClothingRackBlockEntity::new, Set.of(CLOTHING_RACK.get())));
    public static final RegistrySupplier<BlockEntityType<WardrobeBlockEntity>> WARDROBE_ENTITY = BLOCK_ENTITIES.register(
            "wardrobe", () -> new BlockEntityType<>(WardrobeBlockEntity::new, Set.of(WARDROBE.get())));

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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    private static RegistrySupplier<Block> block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        RegistrySupplier<Block> block = BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, id(name)))));
        FemboyItems.blockItem(name, block);
        return block;
    }

    /** Rose quartz bud/cluster: amethyst-style crystal on any face (height and side inset in pixels). */
    private static RegistrySupplier<Block> cluster(String name, float height, float inset, SoundType sound, int light) {
        return block(name, props -> new AmethystClusterBlock(height, inset, props),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).forceSolidOn().noOcclusion().sound(sound).strength(1.5F)
                        .lightLevel(state -> light).pushReaction(PushReaction.POPPED));
    }

    /** Shapes match tools/plush/make_plushies.py (face pointing north). */
    private static RegistrySupplier<Block> plush(String name, VoxelShape northShape) {
        return block(name, props -> new PlushBlock(props, northShape),
                BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noOcclusion());
    }

    /** Called from FemboyMod.init after the block register. */
    public static void init() {
        BLOCK_ENTITIES.register();
        PROFESSIONS.register();
    }
}
