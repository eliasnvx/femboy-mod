package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.GlowHostilesClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code femboymod:muffle_sounds} (Cat-ear Headphones muffle creepers): scales the volume of the listed sounds for
 * the wearer. No loader event exists for per-sound volume, so this is a tiny return-value tweak.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMuffleMixin {

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void femboymod$muffle(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float factor = GlowHostilesClient.volumeFactor(sound.getIdentifier());
        if (factor != 1.0F) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }
}
