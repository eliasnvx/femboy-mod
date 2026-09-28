package dev.eliasnvx.femboymod.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Recipe inputs for tests. 1.20.1 has no {@code CraftingInput} / {@code SmithingRecipeInput}; recipes read containers. */
final class TestContainers {

    /** Smithing table slots: template, base, addition. */
    private static final int SMITHING_SLOTS = 3;

    private TestContainers() {
    }

    /** A {@code width} x {@code height} crafting grid filled row by row with {@code items}. */
    static CraftingContainer crafting(int width, int height, List<ItemStack> items) {
        NonNullList<ItemStack> grid = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            grid.set(i, items.get(i));
        }
        return new TransientCraftingContainer(new NoMenu(), width, height, grid);
    }

    /** The three smithing inputs as the smithing menu passes them to recipes. */
    static SimpleContainer smithing(ItemStack template, ItemStack base, ItemStack addition) {
        SimpleContainer container = new SimpleContainer(SMITHING_SLOTS);
        container.setItem(0, template);
        container.setItem(1, base);
        container.setItem(2, addition);
        return container;
    }

    /** Menu that owns the grid; the grid only notifies it when a slot changes. */
    private static final class NoMenu extends AbstractContainerMenu {

        private NoMenu() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    }
}
