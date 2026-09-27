package dev.eliasnvx.femboymod.client.gui;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Vanilla-looking panels and slots drawn with fills, so layouts live in code, not in a texture. */
public final class FemboyGui {

    public static final int SLOT = 18;
    private static final int ICON = 16;
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int HOVER = 0x80FFFFFF;
    public static final int TEXT = 0xFF404040;
    public static final int DRIP = 0xFFB0407A;
    public static final int SET = 0xFF7A3FB0;
    private static final int BAR_BACK = 0xFF5A4A55;
    private static final int BAR_FILL = 0xFFE07AB0;
    private static final int BAR_SHINE = 0xFFF4B6D6;

    private FemboyGui() {
    }

    /** A window like vanilla containers: black rounded outline, white/grey bevel, light grey body. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, OUTLINE);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + 1, y + h - 1, OUTLINE);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
    }

    /** An 18x18 sunken slot frame; the item goes at (x + 1, y + 1). */
    public static void slotFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + SLOT - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT - 1, SLOT_DARK);
        g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
    }

    /** A sunken box (the doll's window, info area). */
    public static void inset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** Eye toggle in a slot's top-right corner (frame origin x, y). */
    public static final int EYE_X = 11;
    public static final int EYE_Y = 1;
    public static final int EYE_W = 6;
    public static final int EYE_H = 5;
    private static final int EYE_BACK = 0xE0303030;
    private static final int EYE_WHITE = 0xFFF2F2F2;
    private static final int EYE_PUPIL = 0xFF202020;
    private static final int EYE_CROSS = 0xFFE05A7A;
    private static final int HIDDEN_SHADE = 0x90202028;

    public static boolean onEye(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x + EYE_X && mouseX < x + EYE_X + EYE_W && mouseY >= y + EYE_Y && mouseY < y + EYE_Y + EYE_H;
    }

    /** Open eye (visible) or a crossed one (hidden). */
    public static void eye(GuiGraphics g, int x, int y, boolean hidden) {
        int ex = x + EYE_X;
        int ey = y + EYE_Y;
        g.fill(ex, ey, ex + EYE_W, ey + EYE_H, EYE_BACK);
        if (hidden) {
            g.fill(ex + 1, ey + 2, ex + EYE_W - 1, ey + 3, EYE_WHITE);
            for (int i = 0; i < EYE_H; i++) {
                g.fill(ex + i + 1, ey + i, ex + i + 2, ey + i + 1, EYE_CROSS);
            }
        } else {
            g.fill(ex + 1, ey + 1, ex + EYE_W - 1, ey + EYE_H - 1, EYE_WHITE);
            g.fill(ex + 2, ey + 1, ex + 4, ey + EYE_H - 1, EYE_PUPIL);
        }
    }

    /** Darkens a hidden item inside its slot. */
    public static void hiddenShade(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, HIDDEN_SHADE);
    }

    /** Worn item or the slot's empty silhouette, plus a hover highlight and the eye toggle. */
    public static void cosmeticSlot(GuiGraphics g, Font font, ResourceLocation slot, ItemStack stack, int x, int y, boolean hovered,
                                    boolean hidden) {
        slotFrame(g, x, y);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1);
        } else {
            emptySlotIcon(g, CosmeticsManager.slotType(slot).emptySlotIcon().orElse(null), x + 1, y + 1);
        }
        if (hidden && !stack.isEmpty()) {
            hiddenShade(g, x, y);
        }
        if (hovered) {
            g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, HOVER);
        }
        if (hidden || hovered && !stack.isEmpty()) {
            eye(g, x, y, hidden);
        }
    }

    /** The slot type's empty silhouette (a GUI sprite) at the item position, if it has one. */
    public static void emptySlotIcon(GuiGraphics g, @Nullable ResourceLocation sprite, int itemX, int itemY) {
        if (sprite != null) {
            g.blitSprite(sprite, itemX, itemY, ICON, ICON);
        }
    }

    /** Pink progress bar for the Drip Level. */
    public static void dripBar(GuiGraphics g, int x, int y, int w, int h, float fraction) {
        inset(g, x, y, w, h, BAR_BACK);
        int filled = Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, BAR_FILL);
            g.fill(x + 1, y + 1, x + 1 + filled, y + 2, BAR_SHINE);
        }
    }

    public static Component slotName(ResourceLocation slot) {
        return Component.translatable(dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType.translationKey(slot));
    }
}
