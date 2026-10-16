package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.effect.EmulatedAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 1.21.1 {@code player.sneaking_speed} ({@link EmulatedAttributes}): 1.20.1 hard-codes the sneak slowdown in
 * {@code LocalPlayer#aiStep} as {@code 0.3 + Swift Sneak bonus}, the attribute's base plus the enchantment. The
 * cosmetics' bonus is added to that factor, clamped to the attribute's range like on 1.21.1.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSneakMixin {

    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V"), index = 1)
    private float femboymod$sneakingSpeed(float factor) {
        double bonus = EmulatedAttributes.bonus((LocalPlayer) (Object) this, EmulatedAttributes.SNEAKING_SPEED);
        return bonus == 0.0 ? factor : Mth.clamp(factor + (float) bonus, 0.0F, 1.0F);
    }
}
