package dev.eliasnvx.femboymod.effect;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.effect.ConfiguredEffect;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.energy.FemboyEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attributes that cosmetic JSON uses but Minecraft 1.20.1 does not have (they came with 1.20.5-1.21). The
 * {@code femboymod:attribute} effect keeps their 1.21.1 ids; their value is computed here from what the player
 * wears, the way 1.21.1 would compute the attribute, and applied by mixins (LivingEntityAttributesMixin,
 * BlockStateDestroyProgressMixin). Worn cosmetics and their stats are synced, so both sides get the same value:
 * the client needs it for digging and jumping, the server for falling and breath.
 * {@code player.sneaking_speed} is not emulated (the sneak slowdown is hard-coded in the client's LocalPlayer).
 */
public final class EmulatedAttributes {

    /** 1.21.1 {@code player.block_break_speed}, base 1. */
    public static final ResourceLocation BLOCK_BREAK_SPEED = new ResourceLocation("player.block_break_speed");
    /** 1.21.1 {@code generic.safe_fall_distance}, base 3 blocks. */
    public static final ResourceLocation SAFE_FALL_DISTANCE = new ResourceLocation("generic.safe_fall_distance");
    /** 1.21.1 {@code generic.jump_strength}, base 0.42. */
    public static final ResourceLocation JUMP_STRENGTH = new ResourceLocation("generic.jump_strength");
    /** 1.21.1 {@code generic.oxygen_bonus}, base 0 (works like Respiration levels). */
    public static final ResourceLocation OXYGEN_BONUS = new ResourceLocation("generic.oxygen_bonus");

    /** Vanilla 1.21.1 base values of the emulated attributes. */
    private static final Map<ResourceLocation, Double> BASE = Map.of(
            BLOCK_BREAK_SPEED, 1.0,
            SAFE_FALL_DISTANCE, 3.0,
            JUMP_STRENGTH, 0.42,
            OXYGEN_BONUS, 0.0);

    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    private EmulatedAttributes() {
    }

    /** @return whether {@code id} is one of the attributes computed here */
    public static boolean isEmulated(ResourceLocation id) {
        return BASE.containsKey(id);
    }

    /** @return the vanilla base value of an emulated attribute */
    public static double base(ResourceLocation id) {
        return BASE.getOrDefault(id, 0.0);
    }

    /** Logs once per id that an attribute from the data does not exist on 1.20.1 and is ignored. */
    public static void warnMissing(ResourceLocation id) {
        if (WARNED.add(id)) {
            FemboyMod.LOGGER.warn("Attribute {} does not exist on Minecraft 1.20.1; cosmetic effects using it are ignored", id);
        }
    }

    /**
     * The attribute's value for {@code player}: its base plus every active {@code femboymod:attribute} effect on it,
     * in vanilla order (add value, then add multiplied base, then multiply total).
     */
    public static double value(Player player, ResourceLocation id) {
        return value(player, id, 0.0);
    }

    /**
     * Dig speed factor: {@code player.block_break_speed} from the worn cosmetics together with the Insight effect,
     * which 1.21.1 applies as a modifier on the same attribute.
     */
    public static float blockBreakSpeed(Player player) {
        return (float) value(player, BLOCK_BREAK_SPEED, FemboyEffects.insightMultipliedBase(player));
    }

    /** @param extraMultipliedBase further "add multiplied base" amounts from outside the cosmetics */
    private static double value(Player player, ResourceLocation id, double extraMultipliedBase) {
        double base = base(id);
        double added = 0.0;
        double multipliedBase = extraMultipliedBase;
        double total = 1.0;
        List<WornEvaluator.PlannedEffect> effects = WornEvaluator.evaluate(player).effects();
        for (int i = 0; i < effects.size(); i++) { // hot path (digging, jumping): no iterator
            WornEvaluator.PlannedEffect planned = effects.get(i);
            ConfiguredEffect configured = planned.configured();
            if (!(configured.effect() instanceof BuiltinEffects.AttributeEffect effect) || !effect.attribute().equals(id)
                    || configured.when().isPresent() && !configured.when().get().test(player)) {
                continue;
            }
            double amount = effect.amount() * planned.source().scale();
            AttributeModifier.Operation operation = effect.operation();
            if (operation == AttributeModifier.Operation.ADDITION) {
                added += amount;
            } else if (operation == AttributeModifier.Operation.MULTIPLY_BASE) {
                multipliedBase += amount;
            } else {
                total *= 1.0 + amount;
            }
        }
        double value = base + added;
        return (value + value * multipliedBase) * total;
    }

    /** @return {@link #value} minus the base: how much the worn cosmetics add */
    public static double bonus(Player player, ResourceLocation id) {
        return value(player, id) - base(id);
    }
}
