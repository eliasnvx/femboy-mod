package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.effect.EmulatedAttributes;
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
 * the attribute: safe fall distance, jump strength and oxygen bonus.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {

    /** Vanilla 1.20.1 jump power per block jump factor (1.21.1: the jump_strength base). */
    private static final float BASE_JUMP_POWER = 0.42F;

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
