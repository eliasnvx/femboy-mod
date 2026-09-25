package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CosmeticSlot extends Slot {

    private final CosmeticsContainer cosmetics;
    private final Player player;

    public CosmeticSlot(CosmeticsContainer container, Player player, int index, int x, int y) {
        super(container, index, x, y);
        this.cosmetics = container;
        this.player = player;
    }

    public Identifier slotId() {
        return cosmetics.slotId(getContainerSlot());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return CosmeticsManager.canEquip(player, slotId(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return CosmeticsManager.slotType(slotId()).emptySlotIcon().orElse(null);
    }
}
