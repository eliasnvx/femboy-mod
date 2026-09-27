package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.wardrobe.WardrobeMenu;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Wardrobe screen: storage + a side panel with 5 presets ("put on" / "save") and the Drip Level (SPEC §6.1). */
public final class WardrobeScreen extends AbstractContainerScreen<WardrobeMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int WIDTH = 176;
    private static final int HEADER_HEIGHT = 17;
    private static final int INVENTORY_PART_V = 126;
    private static final int INVENTORY_PART_HEIGHT = 96;
    private static final int PANEL_WIDTH = 58;
    private static final int PANEL_GAP = 4;
    private static final int BUTTON_HEIGHT = 18;
    private static final int APPLY_WIDTH = 34;
    private static final int SAVE_WIDTH = 18;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF555555;
    private static final int DRIP_COLOR = 0xFFB0407A;

    public WardrobeScreen(WardrobeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = 114 + WardrobeMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos - PANEL_WIDTH - PANEL_GAP + 3;
        for (int i = 0; i < WardrobePresets.COUNT; i++) {
            int index = i;
            int y = topPos + 18 + i * (BUTTON_HEIGHT + 2);
            addRenderableWidget(Button.builder(Component.translatable("gui.femboymod.wardrobe.preset", i + 1),
                            b -> click(WardrobeMenu.APPLY_BUTTON + index))
                    .bounds(x, y, APPLY_WIDTH, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.femboymod.wardrobe.apply.tooltip")))
                    .build());
            addRenderableWidget(Button.builder(Component.literal("✎"), b -> click(WardrobeMenu.SAVE_BUTTON + index))
                    .bounds(x + APPLY_WIDTH + 1, y, SAVE_WIDTH, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.femboymod.wardrobe.save.tooltip")))
                    .build());
        }
    }

    private void click(int buttonId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int top = WardrobeMenu.ROWS * 18 + HEADER_HEIGHT;
        graphics.blit(BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(BACKGROUND, leftPos, topPos + top, 0.0F, INVENTORY_PART_V,
                imageWidth, INVENTORY_PART_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        int x0 = leftPos - PANEL_WIDTH - PANEL_GAP;
        int y0 = topPos;
        int h = 18 + WardrobePresets.COUNT * (BUTTON_HEIGHT + 2) + 4;
        graphics.fill(x0 - 1, y0 - 1, x0 + PANEL_WIDTH + 1, y0 + h + 1, PANEL_BORDER);
        graphics.fill(x0, y0, x0 + PANEL_WIDTH, y0 + h, PANEL_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.femboymod.wardrobe.presets"), -PANEL_WIDTH - PANEL_GAP + 4, 6, 0xFF404040, false);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var drip = WornEvaluator.evaluate(player).drip();
            Component text = Component.translatable("hud.femboymod.drip", drip.level(), drip.tier());
            graphics.drawString(font, text, imageWidth - 8 - font.width(text), titleLabelY, DRIP_COLOR, false);
        }
    }
}
