package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderStateAccess;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies what a player wears into its render state (the layer only sees the state in 26.x). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void femboymod$extractCosmetics(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((CosmeticRenderStateAccess) state).femboymod$setCosmetics(CosmeticRenderData.capture(entity, state));
    }
}
