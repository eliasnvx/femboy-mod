package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.render.MirrorRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirror reflections have no name tag (1.21.1 draws it from the entity renderer; 26.3 clears it in the render
 * state instead).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameTagMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void femboymod$noNameInMirror(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MirrorRenderer.drawingReflection()) {
            cir.setReturnValue(false);
        }
    }
}
