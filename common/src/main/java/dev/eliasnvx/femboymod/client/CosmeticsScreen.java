package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.client.gui.FemboyGui;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.menu.CosmeticSlot;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * Wardrobe (SPEC §4.6): cosmetic slots around a player doll that follows the mouse, a Drip card with the
 * level, tier and a progress bar, the active set bonuses, and the player inventory below.
 */
public final class CosmeticsScreen extends AbstractContainerScreen<CosmeticsMenu> {

    private static final int DOLL_BACKGROUND = 0xFF1D1A22;
    private static final int INFO_BACKGROUND = 0xFFE9DDE6;
    private static final int DOLL_SCALE = 34;
    private static final float DOLL_Y_OFFSET = 0.0625F;
    private static final int INFO_PADDING = 4;
    private static final int LINE = 10;
    private static final int BAR_HEIGHT = 6;

    public CosmeticsScreen(CosmeticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CosmeticsMenu.WIDTH, menu.height());
        this.inventoryLabelY = menu.inventoryTop() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        FemboyGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int dollX = leftPos + CosmeticsMenu.DOLL_X;
        int top = topPos + CosmeticsMenu.TOP;
        FemboyGui.inset(g, dollX, top, CosmeticsMenu.DOLL_W, CosmeticsMenu.SIDE_H, DOLL_BACKGROUND);
        FemboyGui.inset(g, leftPos + CosmeticsMenu.INFO_X, top, CosmeticsMenu.INFO_W, CosmeticsMenu.SIDE_H, INFO_BACKGROUND);
        for (Slot slot : menu.slots) {
            FemboyGui.slotFrame(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, dollX + 1, top + 1, dollX + CosmeticsMenu.DOLL_W - 1,
                    top + CosmeticsMenu.SIDE_H - 1, DOLL_SCALE, DOLL_Y_OFFSET, mouseX, mouseY, player);
        }
    }

    /** Title, the Drip card and the active sets (SPEC §4.6). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        WornEvaluator.Evaluation evaluation = WornEvaluator.evaluate(player);
        int x = CosmeticsMenu.INFO_X + INFO_PADDING;
        int width = CosmeticsMenu.INFO_W - INFO_PADDING * 2;
        int y = CosmeticsMenu.TOP + INFO_PADDING;
        g.text(font, Component.translatable("gui.femboymod.wardrobe.drip"), x, y, FemboyGui.TEXT, false);
        y += LINE;
        g.text(font, Component.literal(String.valueOf(evaluation.drip().level())), x, y, FemboyGui.DRIP, false);
        Component tier = Component.translatable("gui.femboymod.wardrobe.tier", evaluation.drip().tier());
        g.text(font, tier, x + width - font.width(tier), y, FemboyGui.TEXT, false);
        y += LINE;
        int max = player.level().registryAccess().lookup(DripRules.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(DripRules.DEFAULT)).orElse(DripRules.FALLBACK).maxLevel();
        FemboyGui.dripBar(g, x, y, width, BAR_HEIGHT, evaluation.drip().level() / (float) max);
        y += BAR_HEIGHT + INFO_PADDING;
        if (evaluation.activeSets().isEmpty()) {
            g.text(font, Component.translatable("gui.femboymod.wardrobe.no_sets"), x, y, FemboyGui.TEXT, false);
        }
        for (Identifier set : evaluation.activeSets()) {
            if (y > CosmeticsMenu.TOP + CosmeticsMenu.SIDE_H - LINE) {
                break;
            }
            for (var line : font.split(Component.literal("✦ ").append(Component.translatable(set.toLanguageKey("set_bonus"))), width)) {
                g.text(font, line, x, y, FemboyGui.SET, false);
                y += LINE;
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Name empty cosmetic slots so players know what goes where.
        if (menu.getCarried().isEmpty() && hoveredSlot instanceof CosmeticSlot slot && !slot.hasItem()) {
            g.setTooltipForNextFrame(font, FemboyGui.slotName(slot.slotId()), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }
}
