package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.GlowHostilesClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code femboymod:muffle_sounds} (Cat-ear Headphones muffle creepers): scales the volume of the listed sounds for
 * the wearer. No loader event exists for per-sound volume, so this is a tiny return-value tweak.
 *
 * <p>1.20.1: {@code play} computes the starting volume with {@code calculateVolume(float, SoundSource)}, which does
 * not see the sound instance, so that call is redirected too; volume updates of playing sounds go through
 * {@code calculateVolume(SoundInstance)}.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMuffleMixin {

    @Shadow
    private float calculateVolume(float volume, SoundSource source) {
        throw new AssertionError();
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void femboymod$muffle(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float factor = GlowHostilesClient.volumeFactor(sound.getLocation());
        if (factor != 1.0F) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }

    @Redirect(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float femboymod$muffleOnPlay(SoundEngine engine, float volume, SoundSource source, SoundInstance sound) {
        return calculateVolume(volume, source) * GlowHostilesClient.volumeFactor(sound.getLocation());
    }
}
