package dev.eliasnvx.femboymod.api.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.world.entity.player.Player;

/**
 * A predicate on the wearer that gates a {@link ConfiguredEffect} ("only in cold biomes", "only while
 * crouching"). Types are registered via {@link FemboyApi#cosmeticConditionTypes()}.
 *
 * <p>Evaluated every tick for active sources, so implementations must be cheap and free of side effects.
 * May be evaluated on either logical side.
 */
public interface CosmeticCondition {

    /** Dispatch codec over {@link FemboyApi#cosmeticConditionTypes()}. */
    Codec<CosmeticCondition> CODEC = Codec.lazyInitialized(() -> FemboyApi.get().cosmeticConditionTypes().byIdCodec()
            .dispatch("type", CosmeticCondition::codec, codec -> codec));

    /**
     * @return the codec of this condition's type, as registered
     */
    MapCodec<? extends CosmeticCondition> codec();

    /**
     * @param player the wearer
     * @return whether the gated effect should be active
     */
    boolean test(Player player);
}
