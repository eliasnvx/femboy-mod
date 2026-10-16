package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.client.render.pet.PetOutfitRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements PetOutfitRenderState {

    @Unique
    private ItemStack femboymod$petHead = ItemStack.EMPTY;
    @Unique
    private ItemStack femboymod$petNeck = ItemStack.EMPTY;

    @Override
    public ItemStack femboymod$petHead() {
        return femboymod$petHead;
    }

    @Override
    public ItemStack femboymod$petNeck() {
        return femboymod$petNeck;
    }

    @Override
    public void femboymod$setPetOutfit(ItemStack head, ItemStack neck) {
        femboymod$petHead = head;
        femboymod$petNeck = neck;
    }
}
