package dev.eliasnvx.femboymod.effect;

import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.effect.CosmeticCondition;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
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
        registry.register(id("no_shoes"), NoShoes.CODEC);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(FemboyMod.MOD_ID, path);
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
            return level.getBiome(pos).value().coldEnoughToSnow(pos);
        }
    }

    /** Nothing on the feet: the feet slot is empty and no boots are worn (hidden ones count, they're still on). */
    public enum NoShoes implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<NoShoes> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<NoShoes> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            return player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                    && CosmeticsManager.worn(player).get(FemboySlots.FEET).isEmpty();
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
            return player.level().isNight();
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
