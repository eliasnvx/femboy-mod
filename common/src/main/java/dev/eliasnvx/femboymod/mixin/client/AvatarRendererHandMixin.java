package dev.eliasnvx.femboymod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.client.render.FirstPersonSleeves;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the hoodie sleeve over the first-person hand (both loaders; NeoForge's RenderArmEvent is not needed). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererHandMixin {

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void femboymod$sleeve(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
                                  ModelPart arm, boolean hasSleeve, CallbackInfo ci) {
        FirstPersonSleeves.submit(((AvatarRenderer<?>) (Object) this).getModel(), arm, poseStack, collector, light);
    }
}
