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
import net.minecraft.util.Util;

/**
 * Photo Mode (SPEC v1.1): the Phone flips the camera to a selfie, hides the HUD for a moment, grabs the frame and
 * turns it into an instant-camera print: a square shot in a cream frame with a wide bottom margin carrying the
 * mod's watermark. Saved to {@code screenshots/}. No flash effect.
 */
public final class PhotoMode {

    /** Ticks for the camera and the player's pose to settle before the shot. */
    private static final int SETTLE_TICKS = 6;
    private static final int PAPER_COLOR = 0xFFFAF6EE;
    private static final int PAPER_EDGE_COLOR = 0xFFE9E2D6;
    /** Print margins as a share of the photo's side: thin on three sides, wide at the bottom like an instant print. */
    private static final float SIDE_MARGIN = 0.06F;
    private static final float BOTTOM_MARGIN = 0.26F;
    /** Watermark height as a share of the bottom margin. */
    private static final float WATERMARK_HEIGHT = 0.3F;
    private static final ResourceLocation WATERMARK = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/gui/photo_watermark.png");

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
        hudWasHidden = minecraft.gui.hud.isHidden();
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (!hudWasHidden) {
            minecraft.gui.hud.toggle();
        }
        countdown = SETTLE_TICKS;
    }

    public static void tick(Minecraft minecraft) {
        if (countdown < 0) {
            return;
        }
        if (countdown-- > 0) {
            return;
        }
        Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> save(minecraft, image));
        minecraft.options.setCameraType(previousCamera);
        if (!hudWasHidden && minecraft.gui.hud.isHidden()) {
            minecraft.gui.hud.toggle();
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
                        .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file.getAbsoluteFile())));
                minecraft.execute(() -> minecraft.showDebugChat(Component.translatable("message.femboymod.selfie", name)));
            } catch (Exception e) {
                FemboyMod.LOGGER.warn("Couldn't save selfie", e);
            }
        });
    }

    /** A new image: the centre square of the shot on cream paper, watermark centred on the wide bottom margin. */
    static NativeImage print(Minecraft minecraft, NativeImage shot) {
        int side = Math.min(shot.getWidth(), shot.getHeight());
        int srcX = (shot.getWidth() - side) / 2;
        int srcY = (shot.getHeight() - side) / 2;
        int margin = Math.round(side * SIDE_MARGIN);
        int bottom = Math.round(side * BOTTOM_MARGIN);
        int width = side + 2 * margin;
        int height = side + margin + bottom;
        NativeImage print = new NativeImage(width, height, false);
        print.fillRect(0, 0, width, height, PAPER_COLOR);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                print.setPixel(margin + x, margin + y, shot.getPixel(srcX + x, srcY + y));
            }
        }
        // a hairline around the photo and the paper edge, like a real print
        for (int x = 0; x < width; x++) {
            print.setPixel(x, 0, PAPER_EDGE_COLOR);
            print.setPixel(x, height - 1, PAPER_EDGE_COLOR);
        }
        for (int y = 0; y < height; y++) {
            print.setPixel(0, y, PAPER_EDGE_COLOR);
            print.setPixel(width - 1, y, PAPER_EDGE_COLOR);
        }
        try (InputStream in = minecraft.getResourceManager().open(WATERMARK); NativeImage mark = NativeImage.read(in)) {
            int scale = Math.max(1, Math.round(bottom * WATERMARK_HEIGHT / mark.getHeight()));
            int x0 = (width - mark.getWidth() * scale) / 2;
            int y0 = side + margin + (bottom - mark.getHeight() * scale) / 2;
            for (int y = 0; y < mark.getHeight() * scale; y++) {
                for (int x = 0; x < mark.getWidth() * scale; x++) {
                    int argb = mark.getPixel(x / scale, y / scale);
                    // the white outline would vanish on paper: draw only the pink letters and heart
                    if ((argb >>> 24) > 0 && (argb & 0xFFFFFF) != 0xFFFFFF && x0 + x >= 0 && x0 + x < width) {
                        print.setPixel(x0 + x, y0 + y, argb);
                    }
                }
            }
        } catch (Exception e) {
            FemboyMod.LOGGER.warn("Selfie watermark missing", e);
        }
        return print;
    }
}
