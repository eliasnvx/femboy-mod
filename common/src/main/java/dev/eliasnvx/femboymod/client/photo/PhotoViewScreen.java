package dev.eliasnvx.femboymod.client.photo;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.photo.PhotoData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A photo held up close: the picture on its card, big, with who took it. */
public final class PhotoViewScreen extends Screen {

    private static final ResourceLocation DEVELOPING = new ResourceLocation(FemboyMod.MOD_ID, "textures/item/photo_developing.png");
    private static final int CARD = 0xFFFAF6EE;
    private static final int CARD_EDGE = 0xFFE0D6C8;
    private static final int TEXT = 0xFF5A4A56;
    private static final int MARGIN = 8;
    private static final int BOTTOM = 26;
    private static final int MAX_SIDE = 256;
    /** Room kept around the card on the screen. */
    private static final int SCREEN_PAD = 20;

    private final PhotoData photo;
    private final Component author;

    public PhotoViewScreen(PhotoData photo) {
        super(Component.translatable("item.femboymod.photo"));
        this.photo = photo;
        this.author = Component.translatable("item.femboymod.photo.author", photo.author());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); // 1.20.1: screens draw their own background
        super.render(g, mouseX, mouseY, partialTick);
        int side = Math.min(MAX_SIDE, Math.min(width - 2 * MARGIN - SCREEN_PAD, height - MARGIN - BOTTOM - SCREEN_PAD));
        int cardW = side + 2 * MARGIN;
        int cardH = side + MARGIN + BOTTOM;
        int x = (width - cardW) / 2;
        int y = (height - cardH) / 2;
        g.fill(x - 1, y - 1, x + cardW + 1, y + cardH + 1, CARD_EDGE);
        g.fill(x, y, x + cardW, y + cardH, CARD);
        ResourceLocation picture = PhotoTextures.texture(photo.id());
        g.blit(picture == null ? DEVELOPING : picture, x + MARGIN, y + MARGIN, 0.0F, 0.0F, side, side, side, side);
        g.drawCenteredString(font, author, width / 2, y + MARGIN + side + (BOTTOM - font.lineHeight) / 2, TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
