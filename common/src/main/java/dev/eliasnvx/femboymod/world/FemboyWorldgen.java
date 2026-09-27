package dev.eliasnvx.femboymod.world;

import dev.architectury.registry.level.biome.BiomeModifications;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Ore generation. Vein sizes, counts and heights are data-driven
 * ({@code data/femboymod/worldgen/{feature,placed_feature}}); the biomes are the tags below.
 */
public final class FemboyWorldgen {

    /** Rose quartz: mountains and cave biomes. */
    public static final TagKey<Biome> HAS_ROSE_QUARTZ_ORE = biomeTag("has_rose_quartz_ore");
    /** Glitter: flower biomes. */
    public static final TagKey<Biome> HAS_GLITTER_ORE = biomeTag("has_glitter_ore");

    public static final ResourceKey<PlacedFeature> ORE_ROSE_QUARTZ = placed("ore_rose_quartz");
    public static final ResourceKey<PlacedFeature> ORE_GLITTER = placed("ore_glitter");
    public static final ResourceKey<PlacedFeature> ORE_MOONSTONE = placed("ore_moonstone");
    public static final ResourceKey<PlacedFeature> ORE_NEON_QUARTZ = placed("ore_neon_quartz");
    public static final ResourceKey<PlacedFeature> ROSE_QUARTZ_GEODE = placed("rose_quartz_geode");
    /** Moonstone and geodes: anywhere in the overworld (deep only, set by the placement). */
    public static final TagKey<Biome> HAS_MOONSTONE_ORE = biomeTag("has_moonstone_ore");
    public static final TagKey<Biome> HAS_ROSE_QUARTZ_GEODE = biomeTag("has_rose_quartz_geode");
    /** Neon quartz: the Nether. */
    public static final TagKey<Biome> HAS_NEON_QUARTZ_ORE = biomeTag("has_neon_quartz_ore");

    private FemboyWorldgen() {
    }

    public static void init() {
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_ROSE_QUARTZ_ORE), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_ROSE_QUARTZ));
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_GLITTER_ORE), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_GLITTER));
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_MOONSTONE_ORE), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_MOONSTONE));
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_NEON_QUARTZ_ORE), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ORE_NEON_QUARTZ));
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_ROSE_QUARTZ_GEODE), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ROSE_QUARTZ_GEODE));
    }

    private static TagKey<Biome> biomeTag(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, name));
    }
}
