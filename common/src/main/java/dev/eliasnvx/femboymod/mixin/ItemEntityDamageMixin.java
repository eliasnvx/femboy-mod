package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 has no {@code damage_resistant} component: a dropped netherite backpack ignores the damage in
 * {@code #femboymod:backpack_immune_to} (cactus, explosions; fire and lava are covered by fire_resistant).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityDamageMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void femboymod$damageResistant(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (FemboyItems.isDamageResistant(((ItemEntity) (Object) this).getItem(), source)) {
            cir.setReturnValue(false);
        }
    }
}
