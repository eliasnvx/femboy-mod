package dev.eliasnvx.femboymod.energy;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
            .addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "effect.jitter"),
                    JITTER_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final RegistrySupplier<MobEffect> INSIGHT = REGISTER.register("insight", () -> new Insight()
            .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "effect.insight"),
                    INSIGHT_BREAK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

    private FemboyEffects() {
    }

    /** Tracks an energy drink's buff; on its very last tick the crash hits (SPEC §5.2). */
    static final class Caffeinated extends MobEffect {
        Caffeinated() {
            super(MobEffectCategory.NEUTRAL, CAFFEINE_COLOR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplifier) {
            return remainingDuration == 1; // vanilla passes the remaining duration here for finite effects
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            EnergyDrinks.crash(level, entity);
            return true;
        }
    }

    static final class Insight extends MobEffect {
        Insight() {
            super(MobEffectCategory.BENEFICIAL, INSIGHT_COLOR);
        }
    }

    static final class Jitter extends MobEffect {
        Jitter() {
            super(MobEffectCategory.HARMFUL, JITTER_COLOR, ParticleTypes.ELECTRIC_SPARK);
        }
    }
}
