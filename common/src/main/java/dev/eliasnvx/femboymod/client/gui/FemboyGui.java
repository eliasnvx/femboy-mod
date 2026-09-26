package dev.eliasnvx.femboymod.client.gui;

import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Vanilla-looking panels and slots drawn with fills, so layouts live in code, not in a texture. */
public final class FemboyGui {

    public static final int SLOT = 18;
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
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
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
    public static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + SLOT - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT - 1, SLOT_DARK);
        g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
    }

    /** A sunken box (the doll's window, info area). */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** Worn item or the slot's empty silhouette, plus a hover highlight. */
    public static void cosmeticSlot(GuiGraphicsExtractor g, Font font, Identifier slot, ItemStack stack, int x, int y, boolean hovered) {
        slotFrame(g, x, y);
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
            g.itemDecorations(font, stack, x + 1, y + 1);
        } else {
            CosmeticsManager.slotType(slot).emptySlotIcon()
                    .ifPresent(icon -> g.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x + 1, y + 1, 16, 16));
        }
        if (hovered) {
            g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, HOVER);
        }
    }

    /** Pink progress bar for the Drip Level. */
    public static void dripBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction) {
        inset(g, x, y, w, h, BAR_BACK);
        int filled = Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, BAR_FILL);
            g.fill(x + 1, y + 1, x + 1 + filled, y + 2, BAR_SHINE);
        }
    }

    public static Component slotName(Identifier slot) {
        return Component.translatable(dev.eliasnvx.femboymod.api.cosmetic.CosmeticSlotType.translationKey(slot));
    }
}
