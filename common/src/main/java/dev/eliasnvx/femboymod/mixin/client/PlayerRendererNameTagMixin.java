package dev.eliasnvx.femboymod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.client.render.MirrorRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mirror reflection has no name tag. 1.20.1 draws it inside {@code PlayerRenderer#render} (26.3 cleared it from
 * the render state), so it is skipped while {@link MirrorRenderer} draws a reflection.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererNameTagMixin {

    @Inject(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void femboymod$noNameTagInMirrors(AbstractClientPlayer player, Component name, PoseStack poseStack,
                                               MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (MirrorRenderer.drawingReflection()) {
            ci.cancel();
        }
    }
}
