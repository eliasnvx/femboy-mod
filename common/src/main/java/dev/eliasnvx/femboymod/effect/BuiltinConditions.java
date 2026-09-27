package dev.eliasnvx.femboymod.effect;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.effect.CosmeticCondition;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Built-in {@link CosmeticCondition} types (no fields: {@code {"type": "femboymod:cold_biome"}}). */
public final class BuiltinConditions {

    private BuiltinConditions() {
    }

    public static void register(ApiRegistry<MapCodec<? extends CosmeticCondition>> registry) {
        registry.register(id("cold_biome"), ColdBiome.CODEC);
        registry.register(id("crouching"), Crouching.CODEC);
        registry.register(id("sprinting"), Sprinting.CODEC);
        registry.register(id("night"), Night.CODEC);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    /** Standing where it would snow (temperature below the snow line), e.g. taiga, mountains. */
    public enum ColdBiome implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<ColdBiome> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<ColdBiome> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            Level level = player.level();
            BlockPos pos = player.blockPosition();
            return level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel());
        }
    }

    /** Dark outside in the wearer's dimension (night in the overworld). */
    public enum Night implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<Night> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Night> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            return player.level().isDarkOutside();
        }
    }

    public enum Crouching implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<Crouching> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Crouching> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            return player.isCrouching();
        }
    }

    public enum Sprinting implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<Sprinting> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Sprinting> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            return player.isSprinting();
        }
    }
}
