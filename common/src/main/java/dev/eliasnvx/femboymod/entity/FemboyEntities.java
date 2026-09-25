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
