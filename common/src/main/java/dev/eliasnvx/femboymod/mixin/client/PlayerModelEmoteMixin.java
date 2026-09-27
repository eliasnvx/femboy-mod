package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.EmoteClient;
import dev.eliasnvx.femboymod.client.render.FirstPersonSleeves;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes pose the arms after vanilla animation; there is no loader hook for model poses. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelEmoteMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void femboymod$emote(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                 float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (FirstPersonSleeves.renderingHand()) {
            return; // the first-person hand keeps its fixed pose
        }
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        EmoteClient.pose(model, entity.getId());
        // 1.21.1 sleeves are separate parts copied from the arms before this point: follow the emote pose
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
    }
}
