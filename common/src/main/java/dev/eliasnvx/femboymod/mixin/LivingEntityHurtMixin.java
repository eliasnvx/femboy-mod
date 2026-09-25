package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.combat.DripCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Drip in combat. Architectury's LIVING_HURT event can only cancel, not change the amount, so the raw damage
 * is scaled here, before armor, invulnerability frames and effects see it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float femboymod$dripDamage(float amount, ServerLevel level, DamageSource source) {
        return DripCombat.modifyIncoming((LivingEntity) (Object) this, level, source, amount);
    }
}
