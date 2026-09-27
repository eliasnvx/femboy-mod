package dev.eliasnvx.femboymod.menu;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CosmeticSlot extends Slot {

    private final CosmeticsContainer cosmetics;
    private final Player player;

    public CosmeticSlot(CosmeticsContainer container, Player player, int index, int x, int y) {
        super(container, index, x, y);
        this.cosmetics = container;
        this.player = player;
    }

    public ResourceLocation slotId() {
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

    /**
     * GUI sprite drawn while the slot is empty. On 1.21.1 {@link Slot#getNoItemIcon()} only takes sprites from
     * block/item atlases, so the screens draw this GUI-atlas sprite themselves.
     */
    public @Nullable ResourceLocation emptySlotSprite() {
        return CosmeticsManager.slotType(slotId()).emptySlotIcon().orElse(null);
    }
}
