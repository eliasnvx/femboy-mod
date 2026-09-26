package dev.eliasnvx.femboymod.client.gui;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.mixin.client.AbstractContainerScreenAccessor;
import dev.eliasnvx.femboymod.network.CosmeticPanelClickPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Cosmetic slots docked to the left of the vanilla inventory. They are drawn here and clicks go to the
 * server ({@link CosmeticPanelClickPayload}); the inventory menu itself stays vanilla. The header button
 * opens the full wardrobe.
 */
public final class CosmeticsPanel extends AbstractWidget {

    private static final int PADDING = 5;
    private static final int HEADER = 14;
    private static final int ROWS = 5;
    private static final int GAP = 2;

    private final InventoryScreen screen;
    private final Runnable openWardrobe;

    public CosmeticsPanel(InventoryScreen screen, Runnable openWardrobe) {
        super(0, 0, 0, 0, Component.translatable("gui.femboymod.cosmetics_button.tooltip"));
        this.screen = screen;
        this.openWardrobe = openWardrobe;
        layout();
    }

    private static List<Identifier> slots() {
        return CosmeticsManager.orderedSlots();
    }

    private int columns() {
        return Math.max(1, (slots().size() + ROWS - 1) / ROWS);
    }

    private void layout() {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        int w = PADDING * 2 + columns() * FemboyGui.SLOT;
        int rows = Math.min(ROWS, slots().size());
        setWidth(w);
        setHeight(PADDING * 2 + HEADER + rows * FemboyGui.SLOT);
        setX(access.femboymod$leftPos() - w - GAP);
        setY(access.femboymod$topPos());
    }

    private int slotX(int index) {
        return getX() + PADDING + (index / ROWS) * FemboyGui.SLOT;
    }

    private int slotY(int index) {
        return getY() + PADDING + HEADER + (index % ROWS) * FemboyGui.SLOT;
    }

    private int slotAt(double mouseX, double mouseY) {
        for (int i = 0; i < slots().size(); i++) {
            if (mouseX >= slotX(i) && mouseX < slotX(i) + FemboyGui.SLOT && mouseY >= slotY(i) && mouseY < slotY(i) + FemboyGui.SLOT) {
                return i;
            }
        }
        return -1;
    }

    private boolean onHeader(double mouseX, double mouseY) {
        return mouseY >= getY() + PADDING && mouseY < getY() + PADDING + HEADER - 2 && mouseX >= getX() && mouseX < getX() + getWidth();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        layout();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var font = Minecraft.getInstance().font;
        FemboyGui.panel(g, getX(), getY(), getWidth(), getHeight());
        boolean headerHovered = onHeader(mouseX, mouseY);
        int headerX = getX() + PADDING;
        FemboyGui.inset(g, headerX, getY() + PADDING, getWidth() - PADDING * 2, HEADER - 2, headerHovered ? 0xFFE7B6D2 : 0xFFD9A3C3);
        Component star = Component.literal("✿");
        g.text(font, star, getX() + (getWidth() - font.width(star)) / 2, getY() + PADDING + 2, FemboyGui.DRIP, false);

        var worn = CosmeticsManager.get(player);
        int hovered = slotAt(mouseX, mouseY);
        for (int i = 0; i < slots().size(); i++) {
            Identifier slot = slots().get(i);
            FemboyGui.cosmeticSlot(g, font, slot, worn.get(slot), slotX(i), slotY(i), i == hovered);
        }

        if (headerHovered) {
            WornEvaluator.Evaluation evaluation = WornEvaluator.evaluate(player);
            g.setTooltipForNextFrame(font, List.of(Component.translatable("gui.femboymod.wardrobe.open"),
                    Component.translatable("hud.femboymod.drip", evaluation.drip().level(), evaluation.drip().tier())
                            .withColor(FemboyGui.DRIP)), java.util.Optional.empty(), mouseX, mouseY);
        } else if (hovered >= 0 && screen.getMenu().getCarried().isEmpty()) {
            ItemStack stack = worn.get(slots().get(hovered));
            if (stack.isEmpty()) {
                g.setTooltipForNextFrame(font, FemboyGui.slotName(slots().get(hovered)), mouseX, mouseY);
            } else {
                g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (onHeader(event.x(), event.y())) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            openWardrobe.run();
            return;
        }
        int index = slotAt(event.x(), event.y());
        if (index >= 0) {
            NetworkManager.sendToServer(new CosmeticPanelClickPayload(slots().get(index), event.hasShiftDown()));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
