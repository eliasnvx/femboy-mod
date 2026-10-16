package dev.eliasnvx.femboymod.compat.curios;

import dev.eliasnvx.femboymod.compat.ExternalSlots;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Curios (soft dependency): our cosmetics in Curios slots count as worn. Which items fit which Curios slot comes from
 * the data pack ({@code data/curios/tags/item}, {@code data/femboymod/curios/entities}). Loaded only with Curios.
 */
public final class CuriosSlots {

    private CuriosSlots() {
    }

    public static void register() {
        ExternalSlots.register((player, action) -> CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values()) {
                visit(handler.getStacks(), action);
                visit(handler.getCosmeticStacks(), action);
            }
        }));
    }

    private static void visit(IDynamicStackHandler stacks, Consumer<ItemStack> action) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            action.accept(stacks.getStackInSlot(i));
        }
    }
}
