package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.backpack.BackpackMenu;
import dev.eliasnvx.femboymod.backpack.BackpackSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Backpack screen: chest background with rows of nine plus a small charm panel on the right. */
public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int WIDTH = 176;
    private static final int BASE_HEIGHT = 114;
    private static final int HEADER_HEIGHT = 17;
    private static final int INVENTORY_PART_V = 126;
    private static final int INVENTORY_PART_HEIGHT = 96;
    private static final int INVENTORY_LABEL_OFFSET = 94;
    /** One slot's frame from generic_54 (first slot of the first row). */
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 17;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF555555;

    private final int rows;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, BASE_HEIGHT + menu.rows() * BackpackMenu.SLOT_SIZE);
        this.rows = menu.rows();
        this.inventoryLabelY = this.imageHeight - INVENTORY_LABEL_OFFSET;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int top = rows * BackpackMenu.SLOT_SIZE + HEADER_HEIGHT;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos + top, 0.0F, INVENTORY_PART_V,
                imageWidth, INVENTORY_PART_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // charm side panel
        int x0 = leftPos + BackpackMenu.CHARM_X - 1 - PANEL_PADDING;
        int y0 = topPos + BackpackMenu.TOP - 1 - PANEL_PADDING;
        int w = BackpackMenu.SLOT_SIZE + 2 * PANEL_PADDING;
        int h = BackpackSpec.CHARM_SLOTS * BackpackMenu.SLOT_SIZE + 2 * PANEL_PADDING;
        graphics.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, PANEL_BORDER);
        graphics.fill(x0, y0, x0 + w, y0 + h, PANEL_COLOR);
        for (int i = 0; i < BackpackSpec.CHARM_SLOTS; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + BackpackMenu.CHARM_X - 1,
                    topPos + BackpackMenu.TOP - 1 + i * BackpackMenu.SLOT_SIZE, SLOT_U, SLOT_V,
                    BackpackMenu.SLOT_SIZE, BackpackMenu.SLOT_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    /** Empty charm slots explain themselves on hover (a label would not fit on the side panel). */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.x == BackpackMenu.CHARM_X) {
            graphics.setTooltipForNextFrame(font, Component.translatable("container.femboymod.charms"), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
