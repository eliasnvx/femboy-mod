package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * First-person cue for reactive ears: the wearer can't see their own ears, so when they notice something (or
 * flatten) their tips hang into view from the top edge of the screen, shifted toward the side of what they heard.
 * Drawn from the ear's pixel mask in the worn colors; fades in and out smoothly, hidden with F1.
 */
public final class EarTipsHud {

    /** GUI pixels per mask pixel. */
    private static final int PIXEL = 3;
    /** Ears sit this share of the screen width left and right of their center... */
    private static final float SPREAD = 0.15F;
    /** ...and the center moves this share of the width toward what they noticed (they stay clear of the corners). */
    private static final float SIDE_SHIFT = 0.15F;
    /** At most this share of an ear hangs into view. */
    private static final float PEEK = 0.8F;
    /** The ear on the noticed side peeks this much further. */
    private static final float NEAR_SIDE_BIAS = 0.35F;
    /** Flattened ears show a little less than alert ones and swing outward. */
    private static final float FLATTEN_SHOW = 0.8F;
    private static final float FLATTEN_SWING = 0.6F;
    private static final float MIN_SHOW = 0.03F;
    private static final float FADE = 1.6F;

    private EarTipsHud() {
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientConfig config = FemboyConfig.client();
        // Architectury's HUD event also fires with the HUD hidden (F1), so check it here
        if (player == null || !config.earTips() || !config.reactiveCosmetics() || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson() || player.isSpectator()) {
            return;
        }
        CosmeticRenderData data = CosmeticRenderData.of(player);
        if (data == null) {
            return;
        }
        BuiltinCosmeticRenderers.EarLook look = null;
        Colorway colorway = null;
        List<CosmeticRenderData.Worn> worn = data.worn();
        for (int i = 0; i < worn.size() && look == null; i++) {
            look = BuiltinCosmeticRenderers.earLook(worn.get(i).renderer());
            colorway = worn.get(i).colorway();
        }
        if (look == null) {
            return;
        }
        // In first person the cosmetic layer doesn't draw the local player, so the reactions are smoothed here.
        float partial = delta.getGameTimeDeltaPartialTick(true);
        data.updateReactions(player.tickCount + partial, player.getViewYRot(partial));
        float flatten = data.earFlatten();
        float show = Math.max(data.earAlert(), flatten * FLATTEN_SHOW);
        if (show < MIN_SHOW) {
            return;
        }

        int main = colorway == null ? look.defaultMain() : colorway.stripeColor(0, ColorwayClock.ticks());
        int alpha = Math.round(Mth.clamp(show * FADE, 0.0F, 1.0F) * 255);
        float side = Mth.clamp(data.earYaw() / Mth.HALF_PI, -1.0F, 1.0F); // negative = left
        float width = g.guiWidth();
        float center = width / 2 + side * width * SIDE_SHIFT;
        drawEar(g, look, main, alpha, center - width * SPREAD, show * (1.0F - NEAR_SIDE_BIAS * side), flatten, -1);
        drawEar(g, look, main, alpha, center + width * SPREAD, show * (1.0F + NEAR_SIDE_BIAS * side), flatten, 1);
    }

    /**
     * One ear hanging tip-down from above the screen, pivoting at its base.
     *
     * @param outward -1 for the left ear, 1 for the right one
     */
    private static void drawEar(GuiGraphics g, BuiltinCosmeticRenderers.EarLook look, int main, int alpha,
                                float x, float show, float flatten, int outward) {
        String[] mask = look.shape().mask();
        int rows = mask.length;
        int columns = mask[0].length();
        int height = rows * PIXEL;
        float peek = Mth.clamp(show, 0.0F, 1.0F) * height * PEEK;
        // tips swing outward like the ears' own tilt, further when flattened
        float angle = -outward * (look.shape().tilt() + FLATTEN_SWING * flatten);
        int accent = BuiltinCosmeticRenderers.shade(main, look.accentShade());
        int dark = BuiltinCosmeticRenderers.fixedColor(Groups.DARK, 0);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, peek - height, 0.0F);
        // rotate the current matrix in place (no Quaternionf per frame); GUI fills only use the pose matrix
        pose.last().pose().rotateZ(angle);
        int left = -columns * PIXEL / 2;
        for (int row = 0; row < rows; row++) {
            String line = mask[row];
            // row 0 is the tip: it hangs lowest
            int y = height - (row + 1) * PIXEL;
            for (int column = 0; column < columns; column++) {
                int rgb = switch (line.charAt(column)) {
                    case 'M' -> main;
                    case 'A' -> accent;
                    case 'F' -> look.detail();
                    case 'D' -> dark;
                    default -> -1;
                };
                if (rgb >= 0) {
                    int px = left + column * PIXEL;
                    g.fill(px, y, px + PIXEL, y + PIXEL, ARGB32.color(alpha, rgb));
                }
            }
        }
        pose.popPose();
    }
}
