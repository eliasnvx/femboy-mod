package dev.eliasnvx.femboymod.block;

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
import net.minecraft.world.level.block.Block;
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    private static RegistrySupplier<Block> block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        RegistrySupplier<Block> block = BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, id(name)))));
        FemboyItems.blockItem(name, block);
        return block;
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
