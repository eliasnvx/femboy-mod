package dev.eliasnvx.femboymod.client.render.pet;

import net.minecraft.world.item.ItemStack;

/** Duck interface mixed into LivingEntityRenderState: the pet outfit pieces a pet wears (empty for everything else). */
public interface PetOutfitRenderState {

    ItemStack femboymod$petHead();

    ItemStack femboymod$petNeck();

    void femboymod$setPetOutfit(ItemStack head, ItemStack neck);
}
