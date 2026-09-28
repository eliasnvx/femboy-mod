package dev.eliasnvx.femboymod.energy;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class FemboyEffects {

    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.MOB_EFFECT);

    private static final int CAFFEINE_COLOR = 0xF07AB0;
    private static final int JITTER_COLOR = 0xB0E0FF;
    private static final int INSIGHT_COLOR = 0xFFD84A;
    /** Insight (rubber duck debugging): the fix is obvious now, so you dig a bit faster. */
    private static final double INSIGHT_BREAK_SPEED = 0.2;
    /** Jitter: slightly slower attacks. No visual screen effect at all (photosensitivity, SPEC §5.2). */
    private static final double JITTER_ATTACK_SPEED = -0.15;

    public static final RegistrySupplier<MobEffect> CAFFEINATED = REGISTER.register("caffeinated", Caffeinated::new);
    public static final RegistrySupplier<MobEffect> JITTER = REGISTER.register("jitter", () -> new Jitter()
            .addAttributeModifier(Attributes.ATTACK_SPEED, modifierUuid("effect.jitter"),
                    JITTER_ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /**
     * 1.20.1 has no block_break_speed attribute: the Insight bonus is applied to the dig progress instead
     * (EmulatedAttributes#blockBreakSpeed, see {@link #insightMultipliedBase}).
     */
    public static final RegistrySupplier<MobEffect> INSIGHT = REGISTER.register("insight", Insight::new);

    private FemboyEffects() {
    }

    /**
     * The effect itself. 1.20.1 has no effect holders (effects are used directly); kept so call sites match the
     * 1.21.1 branch.
     */
    public static MobEffect holder(RegistrySupplier<MobEffect> effect) {
        return effect.get();
    }

    /**
     * Insight's dig bonus as the 1.21.1 {@code ADD_MULTIPLIED_BASE} amount on {@code player.block_break_speed}:
     * {@code INSIGHT_BREAK_SPEED * (amplifier + 1)}, 0 without the effect.
     */
    public static double insightMultipliedBase(Player player) {
        MobEffectInstance insight = player.getEffect(INSIGHT.get());
        return insight == null ? 0.0 : INSIGHT_BREAK_SPEED * (insight.getAmplifier() + 1);
    }

    /** 1.20.1 effect modifiers are keyed by a UUID string; derive a stable one from the name. */
    private static String modifierUuid(String name) {
        return UUID.nameUUIDFromBytes((FemboyMod.MOD_ID + ":" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Tracks an energy drink's buff; on its very last tick the crash hits (SPEC §5.2). */
    static final class Caffeinated extends MobEffect {
        Caffeinated() {
            super(MobEffectCategory.NEUTRAL, CAFFEINE_COLOR);
        }

        @Override
        public boolean isDurationEffectTick(int remainingDuration, int amplifier) {
            return remainingDuration == 1; // vanilla passes the remaining duration here for finite effects
        }

        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level() instanceof ServerLevel level) {
                EnergyDrinks.crash(level, entity);
            }
        }
    }

    static final class Insight extends MobEffect {
        Insight() {
            super(MobEffectCategory.BENEFICIAL, INSIGHT_COLOR);
        }
    }

    static final class Jitter extends MobEffect {
        Jitter() {
            super(MobEffectCategory.HARMFUL, JITTER_COLOR); // 1.20.1: no custom effect particle (1.21.1: electric spark)
        }
    }
}
