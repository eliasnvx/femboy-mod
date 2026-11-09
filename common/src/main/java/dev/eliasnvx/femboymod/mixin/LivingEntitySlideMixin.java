package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.effect.SockSlide;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Sock sliding. 26.3 scales the floor's friction with vanilla's {@code minecraft:friction_modifier} attribute; 1.20.1
 * has no such attribute and no loader event for friction, so the same {@code 1 - (1 - f) * grip} is applied here to
 * the friction {@code travel} reads from the block below. Fabric fills that local from {@code Block#getFriction()},
 * Forge from {@code BlockState#getFriction(level, pos, entity)}: its store, the first float stored after
 * {@code getBlockPosBelowThatAffectsMyMovement()}, is the point both share. The local feeds the ground acceleration and
 * the drag, which both only use it on the ground (26.3 uses 1 in the air), so the result matches 26.3.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySlideMixin implements SockSlide.Slider {

    /** Set by {@link SockSlide#setGrip}: 1 = normal friction. Transient, like 26.3's modifier. */
    @Unique
    private float femboymod$slideGrip = 1.0F;

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 0, slice = @Slice(from = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getBlockPosBelowThatAffectsMyMovement()Lnet/minecraft/core/BlockPos;")))
    private float femboymod$sockSlide(float friction) {
        return femboymod$slideGrip < 1.0F ? SockSlide.effectiveFriction(friction, femboymod$slideGrip) : friction;
    }

    @Override
    public float femboymod$slideGrip() {
        return femboymod$slideGrip;
    }

    @Override
    public void femboymod$setSlideGrip(float grip) {
        femboymod$slideGrip = grip;
    }
}
