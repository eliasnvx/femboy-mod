package dev.eliasnvx.femboymod.entity;

import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FemboyEntities {

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.ENTITY_TYPE);

    public static final ResourceKey<EntityType<?>> PINK_CREEPER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "pink_creeper"));
    public static final RegistrySupplier<EntityType<PinkCreeper>> PINK_CREEPER = REGISTER.register(PINK_CREEPER_KEY.identifier(),
            () -> EntityType.Builder.of(PinkCreeper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(8).notInPeaceful().build(PINK_CREEPER_KEY));

    public static final ResourceKey<EntityType<?>> BUG_KEY = key("bug");
    public static final RegistrySupplier<EntityType<Bug>> BUG = REGISTER.register(BUG_KEY.identifier(),
            () -> EntityType.Builder.of(Bug::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.4F).clientTrackingRange(8).notInPeaceful().build(BUG_KEY));
    /** Where bugs swarm at night; data-driven. */
    public static final TagKey<Biome> BUG_SPAWNS = biomeTag("bug_spawns");

    public static final ResourceKey<EntityType<?>> CAFFEINATED_ZOMBIE_KEY = key("caffeinated_zombie");
    public static final RegistrySupplier<EntityType<CaffeinatedZombie>> CAFFEINATED_ZOMBIE = REGISTER.register(CAFFEINATED_ZOMBIE_KEY.identifier(),
            () -> EntityType.Builder.<CaffeinatedZombie>of(CaffeinatedZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).notInPeaceful().build(CAFFEINATED_ZOMBIE_KEY));
    public static final TagKey<Biome> CAFFEINATED_ZOMBIE_SPAWNS = biomeTag("caffeinated_zombie_spawns");

    public static final ResourceKey<EntityType<?>> HISSY_CAT_KEY = key("hissy_cat");
    public static final RegistrySupplier<EntityType<HissyCat>> HISSY_CAT = REGISTER.register(HISSY_CAT_KEY.identifier(),
            () -> EntityType.Builder.of(HissyCat::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.7F).clientTrackingRange(8).notInPeaceful().build(HISSY_CAT_KEY));
    public static final TagKey<Biome> HISSY_CAT_SPAWNS = biomeTag("hissy_cat_spawns");

    public static final ResourceKey<EntityType<?>> FASHION_CRITIC_KEY = key("fashion_critic");
    public static final RegistrySupplier<EntityType<FashionCritic>> FASHION_CRITIC = REGISTER.register(FASHION_CRITIC_KEY.identifier(),
            () -> EntityType.Builder.of(FashionCritic::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).notInPeaceful().build(FASHION_CRITIC_KEY));
    public static final TagKey<Biome> FASHION_CRITIC_SPAWNS = biomeTag("fashion_critic_spawns");

    /** Flowery biomes (flower forest, cherry grove, meadow, sunflower plains, dappled forest); data-driven. */
    public static final TagKey<Biome> PINK_CREEPER_SPAWNS =
            TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "pink_creeper_spawns"));

    private FemboyEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
    }

    private static TagKey<Biome> biomeTag(String name) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
    }

    /** Natural spawning of a hostile mob in a biome tag, if its config weight is above 0. */
    private static <T extends Monster> void addMonsterSpawn(RegistrySupplier<EntityType<T>> type, TagKey<Biome> biomes,
                                                           CommonConfig.Mob config) {
        SpawnPlacementsRegistry.register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        if (config.spawnWeight() > 0) {
            BiomeModifications.addProperties(ctx -> ctx.hasTag(biomes), (ctx, props) ->
                    props.getSpawnProperties().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(type.get(),
                            UniformInt.of(config.minGroup(), Math.max(config.minGroup(), config.maxGroup()))), config.spawnWeight()));
        }
    }

    public static void init() {
        REGISTER.register();
        CommonConfig.Mobs mobs = FemboyConfig.common().mobs();
        EntityAttributeRegistry.register(BUG, () -> Bug.createAttributes(mobs.bug()));
        addMonsterSpawn(BUG, BUG_SPAWNS, mobs.bug());
        EntityAttributeRegistry.register(CAFFEINATED_ZOMBIE, () -> CaffeinatedZombie.createAttributes(mobs.caffeinatedZombie()));
        addMonsterSpawn(CAFFEINATED_ZOMBIE, CAFFEINATED_ZOMBIE_SPAWNS, mobs.caffeinatedZombie());
        EntityAttributeRegistry.register(HISSY_CAT, () -> HissyCat.createAttributes(mobs.hissyCat()));
        addMonsterSpawn(HISSY_CAT, HISSY_CAT_SPAWNS, mobs.hissyCat());
        EntityAttributeRegistry.register(FASHION_CRITIC, () -> FashionCritic.createAttributes(mobs.fashionCritic()));
        addMonsterSpawn(FASHION_CRITIC, FASHION_CRITIC_SPAWNS, mobs.fashionCritic());
        EntityAttributeRegistry.register(PINK_CREEPER, Creeper::createAttributes);
        SpawnPlacementsRegistry.register(PINK_CREEPER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        CommonConfig.PinkCreeper config = FemboyConfig.common().pinkCreeper();
        if (config.spawnWeight() > 0) {
            BiomeModifications.addProperties(ctx -> ctx.hasTag(PINK_CREEPER_SPAWNS), (ctx, props) ->
                    props.getSpawnProperties().addSpawn(MobCategory.MONSTER,
                            new MobSpawnSettings.SpawnerData(PINK_CREEPER.get(), UniformInt.of(config.minGroup(),
                                    Math.max(config.minGroup(), config.maxGroup()))), config.spawnWeight()));
        }
    }
}
