package dev.eliasnvx.femboymod.mixin;

import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click a cosmetic in the inventory to wear it, like armor. The cosmetic slots are not menu slots,
 * so this has to happen before vanilla moves the stack between inventory and hotbar.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void femboymod$quickEquip(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index >= InventoryMenu.INV_SLOT_START && index < InventoryMenu.USE_ROW_SLOT_END
                && CosmeticPanelActions.quickEquip((InventoryMenu) (Object) this, player, index)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
