package dev.eliasnvx.femboymod.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.eliasnvx.femboymod.FemboyMod;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Draws pixel-art GUI sprites without uneven pixels. A sprite stretched into a fixed box is scaled by a fraction at odd
 * GUI scales (a 32x32 icon in a 16-unit box is 2.5x at scale 5): some pixels come out 2 screen pixels wide, some 3,
 * and thin lines look broken. Here every sprite of one canvas size is drawn at the same whole multiple of its own
 * size, centred on the box.
 *
 * <p>Sprite rule (so all slots of a set keep the same size): a 16x16 sprite fills its canvas; a 32x32 sprite keeps its
 * artwork within the central {@value #HD_ART_NUMERATOR}/{@value #HD_ART_DENOMINATOR} of the canvas (24x24 pixels).
 */
public final class PixelSprites {

    /** Artwork of a canvas larger than 16 pixels is assumed to cover this fraction of it. */
    private static final int HD_ART_NUMERATOR = 3;
    private static final int HD_ART_DENOMINATOR = 4;
    /** Canvases up to this size are assumed to be filled by their artwork (vanilla-style 16x16 icons). */
    private static final int NATIVE_CANVAS = 16;
    /** The artwork should cover this share of the box, in percent. */
    private static final int TARGET_FILL_PERCENT = 80;
    /** The frame of a slot, in GUI units, on each side of the box: artwork may reach into it, not past it. */
    private static final int FRAME = 1;

    private static final Map<ResourceLocation, OptionalInt> CANVAS = new ConcurrentHashMap<>();

    private PixelSprites() {
    }

    /** Forgets measured sprites; runs on every resource reload so edited icons are picked up (F3+T). */
    public static final class Reloader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            CANVAS.clear();
        }
    }

    /**
     * Draws {@code sprite} centred on the {@code box} x {@code box} GUI-unit square whose top-left corner is
     * ({@code x}, {@code y}), at a whole multiple of its pixel size.
     */
    public static void blitFitted(GuiGraphics g, ResourceLocation sprite, int x, int y, int box) {
        int scale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        OptionalInt canvas = CANVAS.computeIfAbsent(sprite, PixelSprites::canvasSize);
        int multiple = canvas.isPresent() ? multiple(canvas.getAsInt(), box, scale) : 0;
        if (multiple < 1) {
            // Unknown sprite, or a GUI scale too small for the artwork: plain stretch
            g.blitSprite(sprite, x, y, box, box);
            return;
        }
        int size = canvas.getAsInt() * multiple;
        // Whole screen pixels from here on: the box centre in screen pixels, minus half the drawn size
        int left = x * scale + box * scale / 2 - size / 2;
        int top = y * scale + box * scale / 2 - size / 2;
        g.pose().pushPose();
        g.pose().translate((float) left / scale, (float) top / scale, 0.0F);
        g.pose().scale((float) multiple / scale, (float) multiple / scale, 1.0F);
        g.blitSprite(sprite, 0, 0, canvas.getAsInt(), canvas.getAsInt());
        g.pose().popPose();
    }

    /**
     * The whole multiple of a {@code canvas}-pixel sprite to draw at GUI scale {@code scale}; 0 if even one times
     * would not fit the slot. Package-private for the unit test.
     */
    static int multiple(int canvas, int box, int scale) {
        if (canvas <= NATIVE_CANVAS) {
            return scale * box / canvas;
        }
        int art = canvas * HD_ART_NUMERATOR / HD_ART_DENOMINATOR;
        int target = box * scale * TARGET_FILL_PERCENT / 100;
        int below = target / art;
        // The multiple whose artwork comes closest to the target size; ties go to the larger one
        int best = below;
        if (below < 1 || Math.abs(art * (below + 1) - target) <= Math.abs(art * below - target)) {
            best = below + 1;
        }
        return art * best <= (box + 2 * FRAME) * scale ? best : 0;
    }

    private static OptionalInt canvasSize(ResourceLocation sprite) {
        ResourceLocation file = sprite.withPath(path -> "textures/gui/sprites/" + path + ".png");
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);
        if (resource == null) {
            return OptionalInt.empty();
        }
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(in)) {
            return OptionalInt.of(Math.max(image.getWidth(), image.getHeight()));
        } catch (IOException e) {
            FemboyMod.LOGGER.debug("Could not read GUI sprite {}", sprite, e);
            return OptionalInt.empty();
        }
    }
}
