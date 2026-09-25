package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.GlowHostilesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cat Ears: hostile mobs nearby glow, for the wearer only (client-side outline). */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void femboymod$glowHostiles(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GlowHostilesClient.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
