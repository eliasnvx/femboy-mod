package dev.eliasnvx.femboymod.api.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.eliasnvx.femboymod.api.FemboyApi;
import net.minecraft.server.level.ServerPlayer;

/**
 * Something a worn cosmetic, a completed set or a charm does to its wearer (attribute modifiers,
 * potion effects, particles, ...). Instances are configured in JSON; their <b>types</b> are
 * registered in Java via {@link FemboyApi#cosmeticEffectTypes()}.
 *
 * <p>JSON form: {@code {"type": "femboymod:attribute", ...type-specific fields}}.
 *
 * <p>Lifecycle on the logical server: {@link #onActivate} when the effect starts applying (item put
 * on, set completed, condition became true), {@link #tick} every server tick while active,
 * {@link #onDeactivate} when it stops. Effects must fully undo themselves in {@link #onDeactivate}
 * (e.g. remove the attribute modifier they added). Implementations must be immutable.
 */
public interface CosmeticEffect {

    /** Dispatch codec over {@link FemboyApi#cosmeticEffectTypes()}. */
    Codec<CosmeticEffect> CODEC = Codec.lazyInitialized(() -> FemboyApi.get().cosmeticEffectTypes().byIdCodec()
            .dispatch("type", CosmeticEffect::codec, codec -> codec));

    /**
     * Returns the codec of this effect's type; must be the instance registered in
     * {@link FemboyApi#cosmeticEffectTypes()}.
     *
     * @return the type codec
     */
    MapCodec<? extends CosmeticEffect> codec();

    /**
     * Called once when the effect becomes active.
     *
     * @param player the wearer
     * @param source where the effect comes from; use {@link EffectSource#id()} to build unique ids
     */
    default void onActivate(ServerPlayer player, EffectSource source) {
    }

    /**
     * Called every server tick while active.
     *
     * @param player the wearer
     * @param source where the effect comes from
     */
    default void tick(ServerPlayer player, EffectSource source) {
    }

    /**
     * Called once when the effect stops (item removed, set broken, condition false, logout, death).
     *
     * @param player the wearer
     * @param source where the effect comes from
     */
    default void onDeactivate(ServerPlayer player, EffectSource source) {
    }
}
