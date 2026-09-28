package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.effect.EmulatedAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Faster digging from cosmetics and Insight. 1.20.1 has no {@code player.block_break_speed} attribute (see
 * EmulatedAttributes), and Forge bypasses {@code Player#getDestroySpeed}, so the dig progress is scaled here, on
 * the path both loaders share. Runs on both sides: the client decides when a block is broken.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroyProgressMixin {

    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void femboymod$breakSpeed(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float multiplier = EmulatedAttributes.blockBreakSpeed(player);
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
