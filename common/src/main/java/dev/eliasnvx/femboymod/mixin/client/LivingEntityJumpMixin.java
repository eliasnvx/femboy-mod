package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.NyaSound;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires {@link NyaSound#onJump} whenever the local player jumps (handles single jumps, bunnyhop and auto-jump). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void femboymod$onJumpFromGround(CallbackInfo ci) {
        NyaSound.onJump((LivingEntity) (Object) this);
    }
}
