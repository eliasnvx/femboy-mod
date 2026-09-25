package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType;
import dev.eliasnvx.femboymod.menu.CosmeticSlot;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Cosmetic slots screen, drawn with the vanilla chest background (rows of nine). */
public final class CosmeticsScreen extends AbstractContainerScreen<CosmeticsMenu> {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int WIDTH = 176;
    /** Height of generic_54's header plus inventory part (as in ContainerScreen). */
    private static final int BASE_HEIGHT = 114;
    private static final int HEADER_HEIGHT = 17;
    private static final int INVENTORY_PART_V = 126;
    private static final int INVENTORY_PART_HEIGHT = 96;
    private static final int INVENTORY_LABEL_OFFSET = 94;

    private final int rows;

    public CosmeticsScreen(CosmeticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, BASE_HEIGHT + menu.rows() * CosmeticsMenu.SLOT_SIZE);
        this.rows = menu.rows();
        this.inventoryLabelY = this.imageHeight - INVENTORY_LABEL_OFFSET;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int top = rows * CosmeticsMenu.SLOT_SIZE + HEADER_HEIGHT;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, top, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos + top, 0.0F, INVENTORY_PART_V,
                imageWidth, INVENTORY_PART_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Name empty cosmetic slots so players know what goes where.
        if (menu.getCarried().isEmpty() && hoveredSlot instanceof CosmeticSlot slot && !slot.hasItem()) {
            graphics.setTooltipForNextFrame(font, Component.translatable(CosmeticSlotType.translationKey(slot.slotId())), mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
