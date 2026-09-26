package dev.eliasnvx.femboymod.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The inventory's position moves with the recipe book; the cosmetics panel follows it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int femboymod$leftPos();

    @Accessor("topPos")
    int femboymod$topPos();
}
