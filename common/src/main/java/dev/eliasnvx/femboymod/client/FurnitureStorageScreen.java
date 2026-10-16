package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.menu.FurnitureStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Laundry Basket and Mini Fridge: a 3x3 grid on the vanilla dispenser background (referenced, not copied). */
public final class FurnitureStorageScreen extends AbstractContainerScreen<FurnitureStorageMenu> {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public FurnitureStorageScreen(FurnitureStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
