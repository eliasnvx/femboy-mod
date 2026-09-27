package dev.eliasnvx.femboymod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.client.render.FirstPersonSleeves;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the hoodie sleeve and hand cosmetics over the first-person hand (both loaders; no loader event needed). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHandMixin {

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void femboymod$handStart(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        FirstPersonSleeves.setRenderingHand(true);
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void femboymod$sleeve(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                  ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        FirstPersonSleeves.setRenderingHand(false);
        FirstPersonSleeves.render(((PlayerRenderer) (Object) this).getModel(), arm, poseStack, buffers, light, player);
    }
}
