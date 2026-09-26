package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.EmoteClient;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes pose the arms after vanilla animation; there is no loader hook for model poses. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelEmoteMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void femboymod$emote(AvatarRenderState state, CallbackInfo ci) {
        EmoteClient.pose((PlayerModel) (Object) this, state);
    }
}
