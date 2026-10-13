package dev.eliasnvx.femboymod.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.eliasnvx.femboymod.FemboyMod;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.Util;

/**
 * Photo Mode (SPEC v1.1): the Phone flips the camera to a selfie, hides the HUD for a moment, grabs the frame and
 * turns it into an instant-camera print: a square shot in a rounded cream card with a wide bottom margin carrying the
 * mod's watermark, on a soft shadow. Saved to {@code screenshots/}. No flash effect.
 */
public final class PhotoMode {

    /** Ticks for the camera and the player's pose to settle before the shot. */
    private static final int SETTLE_TICKS = 6;
    private static final int PAPER_COLOR = 0xFFFAF6EE;
    private static final int PAPER_EDGE_COLOR = 0xFFE9E2D6;
    /** Print margins as a share of the photo's side: thin on three sides, wide at the bottom like an instant print. */
    private static final float SIDE_MARGIN = 0.06F;
    private static final float BOTTOM_MARGIN = 0.26F;
    /** Paper corner radius as a share of the paper width; the photo window has smaller corners (share of its side). */
    private static final float CORNER_RADIUS = 0.035F;
    private static final float PHOTO_CORNER_RADIUS = 0.018F;
    /** Width of the darker hairline along the paper edge, in pixels. */
    private static final float PAPER_EDGE_WIDTH = 1.5F;
    /** Inner shade along the photo edge: width as a share of the photo side, and its strength at the edge. */
    private static final float PHOTO_SHADE_WIDTH = 0.008F;
    private static final float PHOTO_SHADE = 0.22F;
    /** Soft shadow under the card: blur and downward offset as shares of the paper width. */
    private static final float SHADOW_BLUR = 0.035F;
    private static final float SHADOW_DROP = 0.012F;
    private static final float SHADOW_ALPHA = 0.35F;
    private static final int SHADOW_COLOR = 0xFF2A1A22;
    /** Watermark height as a share of the bottom margin. */
    private static final float WATERMARK_HEIGHT = 0.3F;
    private static final ResourceLocation WATERMARK = new ResourceLocation(FemboyMod.MOD_ID, "textures/gui/photo_watermark.png");

    private static int countdown = -1;
    private static CameraType previousCamera;
    private static boolean hudWasHidden;

    private PhotoMode() {
    }

    public static boolean active() {
        return countdown >= 0;
    }

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active() || minecraft.player == null) {
            return;
        }
        previousCamera = minecraft.options.getCameraType();
        hudWasHidden = minecraft.options.hideGui;
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        minecraft.options.hideGui = true;
        countdown = SETTLE_TICKS;
    }

    public static void tick(Minecraft minecraft) {
        if (countdown < 0) {
            return;
        }
        if (countdown-- > 0) {
            return;
        }
        save(minecraft, Screenshot.takeScreenshot(minecraft.getMainRenderTarget()));
        minecraft.options.setCameraType(previousCamera);
        if (!hudWasHidden) {
            minecraft.options.hideGui = false;
        }
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.6F);
        }
    }

    private static void save(Minecraft minecraft, NativeImage shot) {
        NativeImage image = print(minecraft, shot);
        shot.close();
        File dir = new File(minecraft.gameDirectory, Screenshot.SCREENSHOT_DIR);
        File file = new File(dir, "femboymod_selfie_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".png");
        Util.ioPool().execute(() -> {
            try (image) {
                dir.mkdirs();
                image.writeToFile(file);
                Component name = Component.literal(file.getName()).withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
                minecraft.execute(() -> minecraft.gui.getChat().addMessage(Component.translatable("message.femboymod.selfie", name)));
            } catch (Exception e) {
                FemboyMod.LOGGER.warn("Couldn't save selfie", e);
            }
        });
    }

    /** NativeImage pixels here are ABGR; the colours and the print math are ARGB. Swapping red and blue goes both ways. */
    private static int swapRedBlue(int color) {
        return color & 0xFF00FF00 | (color & 0xFF) << 16 | color >> 16 & 0xFF;
    }

    private static int argb(NativeImage image, int x, int y) {
        return swapRedBlue(image.getPixelRGBA(x, y));
    }

    private static void setArgb(NativeImage image, int x, int y, int argb) {
        image.setPixelRGBA(x, y, swapRedBlue(argb));
    }

    /**
     * A new image: the centre square of the shot in a rounded window on rounded cream paper, the watermark centred
     * on the wide bottom margin, and a soft shadow under the card. Corners and shadow are transparent PNG.
     */
    static NativeImage print(Minecraft minecraft, NativeImage shot) {
        int side = Math.min(shot.getWidth(), shot.getHeight());
        int srcX = (shot.getWidth() - side) / 2;
        int srcY = (shot.getHeight() - side) / 2;
        int margin = Math.round(side * SIDE_MARGIN);
        int bottom = Math.round(side * BOTTOM_MARGIN);
        int paperWidth = side + 2 * margin;
        int paperHeight = side + margin + bottom;
        float radius = paperWidth * CORNER_RADIUS;
        float photoRadius = side * PHOTO_CORNER_RADIUS;
        float photoShade = Math.max(1, side * PHOTO_SHADE_WIDTH);
        float blur = paperWidth * SHADOW_BLUR;
        float drop = paperWidth * SHADOW_DROP;
        int pad = (int) Math.ceil(blur + drop);
        int width = paperWidth + 2 * pad;
        int height = paperHeight + 2 * pad;
        NativeImage print = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // pixel centre in paper coordinates
                float px = x - pad + 0.5F;
                float py = y - pad + 0.5F;
                float shadowDistance = InstantPrint.distance(px, py - drop, 0, 0, paperWidth, paperHeight, radius);
                int color = InstantPrint.shadow(shadowDistance, blur, SHADOW_COLOR, SHADOW_ALPHA);
                float paperDistance = InstantPrint.distance(px, py, 0, 0, paperWidth, paperHeight, radius);
                float paper = InstantPrint.coverage(paperDistance);
                if (paper > 0) {
                    int card = InstantPrint.mix(PAPER_COLOR, PAPER_EDGE_COLOR, InstantPrint.edge(paperDistance, PAPER_EDGE_WIDTH));
                    float photoDistance = InstantPrint.distance(px, py, margin, margin, side, side, photoRadius);
                    float photo = InstantPrint.coverage(photoDistance);
                    if (photo > 0) {
                        int sx = Math.max(0, Math.min(side - 1, x - pad - margin));
                        int sy = Math.max(0, Math.min(side - 1, y - pad - margin));
                        int pixel = 0xFF000000 | argb(shot, srcX + sx, srcY + sy);
                        // the photo sits slightly below the paper: a soft inner shade along its edge
                        pixel = InstantPrint.mix(pixel, 0xFF000000, PHOTO_SHADE * InstantPrint.edge(photoDistance, photoShade));
                        card = InstantPrint.mix(card, pixel, photo);
                    }
                    color = InstantPrint.over(card, paper, color);
                }
                setArgb(print, x, y, color);
            }
        }
        try (InputStream in = minecraft.getResourceManager().open(WATERMARK); NativeImage mark = NativeImage.read(in)) {
            int scale = Math.max(1, Math.round(bottom * WATERMARK_HEIGHT / mark.getHeight()));
            int x0 = pad + (paperWidth - mark.getWidth() * scale) / 2;
            int y0 = pad + side + margin + (bottom - mark.getHeight() * scale) / 2;
            for (int y = 0; y < mark.getHeight() * scale; y++) {
                for (int x = 0; x < mark.getWidth() * scale; x++) {
                    int argb = argb(mark, x / scale, y / scale);
                    // the white outline would vanish on paper: draw only the pink letters and heart
                    if ((argb >>> 24) > 0 && (argb & 0xFFFFFF) != 0xFFFFFF && x0 + x >= 0 && x0 + x < width) {
                        setArgb(print, x0 + x, y0 + y, InstantPrint.over(argb, 1, argb(print, x0 + x, y0 + y)));
                    }
                }
            }
        } catch (Exception e) {
            FemboyMod.LOGGER.warn("Selfie watermark missing", e);
        }
        return print;
    }
}
