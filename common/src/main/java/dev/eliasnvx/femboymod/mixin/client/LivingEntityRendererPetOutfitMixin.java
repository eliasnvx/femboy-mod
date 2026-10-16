package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.render.pet.PetOutfitRenderState;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies a pet's outfit into its render state (layers only see the state in 26.x). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererPetOutfitMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void femboymod$extractPetOutfit(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        boolean pet = PetOutfits.canWear(entity);
        ((PetOutfitRenderState) state).femboymod$setPetOutfit(
                pet ? PetOutfits.worn(entity, PetOutfitItem.Slot.HEAD) : ItemStack.EMPTY,
                pet ? PetOutfits.worn(entity, PetOutfitItem.Slot.NECK) : ItemStack.EMPTY);
    }
}
