package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.entity.PinkCreeper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pink Creepers burst into confetti instead of exploding (explodeCreeper is private, so no override). */
@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void femboymod$confetti(CallbackInfo ci) {
        if ((Object) this instanceof PinkCreeper pink && pink.level() instanceof ServerLevel level) {
            pink.confettiBurst(level);
            ci.cancel();
        }
    }
}
