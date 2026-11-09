package dev.eliasnvx.femboymod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.20.1: the block whose friction {@code LivingEntity#travel} uses is protected (1.21.1 made it public). Sock sliding
 * checks the same block for {@code #femboymod:sock_slippery} ({@code SockSlide#onSlipperyFloor}).
 */
@Mixin(Entity.class)
public interface EntityMovementAccessor {

    @Invoker("getBlockPosBelowThatAffectsMyMovement")
    BlockPos femboymod$getBlockPosBelowThatAffectsMyMovement();
}
