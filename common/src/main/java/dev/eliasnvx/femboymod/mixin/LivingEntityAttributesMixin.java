package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.effect.EmulatedAttributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the 1.21.1 attributes that 1.20.1 lacks ({@link EmulatedAttributes}) to players, where 1.21.1 reads
 * the attribute: safe fall distance, jump strength, oxygen bonus, step height and movement efficiency.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {

    /** Vanilla 1.20.1 jump power per block jump factor (1.21.1: the jump_strength base). */
    private static final float BASE_JUMP_POWER = 0.42F;
    /** 1.21.1 attribute ranges: step_height 0..10, movement_efficiency 0..1. */
    private static final double MAX_STEP_HEIGHT = 10.0;
    private static final double MAX_MOVEMENT_EFFICIENCY = 1.0;

    /** generic.safe_fall_distance: a longer safe fall is a shorter fall. */
    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float femboymod$safeFallDistance(float fallDistance) {
        if ((Object) this instanceof Player player) {
            return fallDistance - (float) EmulatedAttributes.bonus(player, EmulatedAttributes.SAFE_FALL_DISTANCE);
        }
        return fallDistance;
    }

    /** generic.jump_strength: 1.21.1 jump power = jump_strength * block jump factor + jump boost. */
    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void femboymod$jumpStrength(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) {
            double bonus = EmulatedAttributes.bonus(player, EmulatedAttributes.JUMP_STRENGTH);
            if (bonus != 0.0) {
                float power = cir.getReturnValueF();
                float blockFactor = (power - player.getJumpBoostPower()) / BASE_JUMP_POWER;
                cir.setReturnValue(power + (float) bonus * blockFactor);
            }
        }
    }

    /**
     * generic.step_height: added to the vanilla step height, which is the attribute's base (0.6). Both loaders read
     * it here: vanilla's Entity#move directly, Forge's through IForgeEntity#getStepHeight.
     */
    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void femboymod$stepHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) {
            double bonus = EmulatedAttributes.bonus(player, EmulatedAttributes.STEP_HEIGHT);
            if (bonus != 0.0) {
                cir.setReturnValue((float) Mth.clamp(cir.getReturnValueF() + bonus, 0.0, MAX_STEP_HEIGHT));
            }
        }
    }

    /** generic.movement_efficiency: 1.21.1 moves the block speed factor toward 1 by the attribute. */
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void femboymod$movementEfficiency(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) {
            double efficiency = EmulatedAttributes.value(player, EmulatedAttributes.MOVEMENT_EFFICIENCY);
            if (efficiency > 0.0) {
                float factor = (float) Mth.clamp(efficiency, 0.0, MAX_MOVEMENT_EFFICIENCY);
                cir.setReturnValue(Mth.lerp(factor, cir.getReturnValueF(), 1.0F));
            }
        }
    }

    /** generic.oxygen_bonus: works like extra Respiration levels (1.21.1 adds Respiration to this attribute). */
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void femboymod$oxygenBonus(int air, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            double bonus = EmulatedAttributes.bonus(player, EmulatedAttributes.OXYGEN_BONUS);
            if (bonus > 0.0) {
                double oxygen = EnchantmentHelper.getRespiration(player) + bonus;
                cir.setReturnValue(player.getRandom().nextDouble() >= 1.0 / (oxygen + 1.0) ? air : air - 1);
            }
        }
    }
}
