package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drip in combat and warm cosmetics in powder snow. Architectury's LIVING_HURT event can only cancel, not change
 * the amount, so the raw damage is scaled here, before armor, invulnerability frames and effects see it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float femboymod$dripDamage(float amount, ServerLevel level, DamageSource source) {
        return DripCombat.modifyIncoming((LivingEntity) (Object) this, level, source, amount);
    }

    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void femboymod$warmCosmeticsPreventFreezing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            for (ItemStack worn : CosmeticsManager.worn(player).all().values()) {
                if (worn.is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}
