package dev.eliasnvx.femboymod.client.gui;

import com.google.common.collect.Lists;
import dev.eliasnvx.femboymod.network.FemboyNetwork;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.mixin.client.AbstractContainerScreenAccessor;
import dev.eliasnvx.femboymod.network.CosmeticPanelClickPayload;
import dev.eliasnvx.femboymod.network.CreativeCosmeticSetPayload;
import dev.eliasnvx.femboymod.network.ToggleCosmeticHiddenPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private final AbstractContainerScreen<?> screen;
    private final Runnable openWardrobe;

    public CosmeticsPanel(AbstractContainerScreen<?> screen, Runnable openWardrobe) {
        super(0, 0, 0, 0, Component.translatable("gui.femboymod.cosmetics_button.tooltip"));
        this.screen = screen;
        this.openWardrobe = openWardrobe;
        layout();
    }

    private static List<ResourceLocation> slots() {
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
        height = PADDING * 2 + HEADER + rows * FemboyGui.SLOT; // 1.20.1 has no setHeight
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
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
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
        g.drawString(font, star, getX() + (getWidth() - font.width(star)) / 2, getY() + PADDING + 2, FemboyGui.DRIP, false);

        var worn = CosmeticsManager.get(player);
        int hovered = slotAt(mouseX, mouseY);
        for (int i = 0; i < slots().size(); i++) {
            ResourceLocation slot = slots().get(i);
            FemboyGui.cosmeticSlot(g, font, slot, worn.get(slot), slotX(i), slotY(i), i == hovered, worn.isHidden(slot));
        }

        if (headerHovered) {
            WornEvaluator.Evaluation evaluation = WornEvaluator.evaluate(player);
            deferTooltip(List.of(Component.translatable("gui.femboymod.wardrobe.open"),
                    Component.translatable("hud.femboymod.drip", evaluation.drip().level(), evaluation.drip().tier())
                            .withStyle(style -> style.withColor(FemboyGui.DRIP))));
        } else if (hovered >= 0 && screen.getMenu().getCarried().isEmpty()) {
            ItemStack stack = worn.get(slots().get(hovered));
            if (!stack.isEmpty() && FemboyGui.onEye(mouseX, mouseY, slotX(hovered), slotY(hovered))) {
                deferTooltip(List.of(Component.translatable(worn.isHidden(slots().get(hovered))
                        ? "gui.femboymod.cosmetic.show" : "gui.femboymod.cosmetic.hide")));
            } else if (stack.isEmpty()) {
                deferTooltip(List.of(FemboyGui.slotName(slots().get(hovered))));
            } else {
                deferTooltip(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            }
        }
    }

    /**
     * The container draws its slots after the widgets with depth testing off, so a tooltip drawn here would end up
     * under them; the screen draws deferred tooltips last.
     */
    private void deferTooltip(List<Component> lines) {
        screen.setTooltipForNextRenderPass(Lists.transform(lines, Component::getVisualOrderText));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onHeader(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            openWardrobe.run();
            return;
        }
        int index = slotAt(mouseX, mouseY);
        Player player = Minecraft.getInstance().player;
        if (index >= 0 && player != null && FemboyGui.onEye(mouseX, mouseY, slotX(index), slotY(index))
                && (!CosmeticsManager.get(player).get(slots().get(index)).isEmpty() || CosmeticsManager.get(player).isHidden(slots().get(index)))) {
            FemboyNetwork.sendToServer(new ToggleCosmeticHiddenPayload(slots().get(index)));
            return;
        }
        if (index >= 0 && player != null) {
            ResourceLocation slot = slots().get(index);
            if (player.getAbilities().instabuild && !Screen.hasShiftDown()) {
                // creative: the cursor is client-side; swap it with the worn item and tell the server
                ItemStack carried = screen.getMenu().getCarried();
                ItemStack worn = CosmeticsManager.get(player).get(slot);
                if (!carried.isEmpty() && !CosmeticsManager.canEquip(player, slot, carried)) {
                    return;
                }
                FemboyNetwork.sendToServer(new CreativeCosmeticSetPayload(slot, carried.copyWithCount(Math.min(1, carried.getCount()))));
                screen.getMenu().setCarried(worn.copy());
            } else {
                FemboyNetwork.sendToServer(new CosmeticPanelClickPayload(slot, Screen.hasShiftDown()));
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
