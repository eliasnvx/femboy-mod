package dev.eliasnvx.femboymod.client;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.emote.Emote;
import dev.eliasnvx.femboymod.network.EmotePayloads;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Emote wheel: one button per emote around the screen centre; picking one plays it and closes the wheel. */
public final class EmoteScreen extends Screen {

    private static final int BUTTON_W = 90;
    private static final int BUTTON_H = 20;
    private static final int RADIUS = 60;

    public EmoteScreen() {
        super(Component.translatable("gui.femboymod.emotes"));
    }

    @Override
    protected void init() {
        Emote[] emotes = Emote.values();
        for (int i = 0; i < emotes.length; i++) {
            Emote emote = emotes[i];
            float angle = -Mth.HALF_PI + i * Mth.TWO_PI / emotes.length;
            int x = width / 2 + Math.round(Mth.cos(angle) * RADIUS) - BUTTON_W / 2;
            int y = height / 2 + Math.round(Mth.sin(angle) * RADIUS) - BUTTON_H / 2;
            addRenderableWidget(Button.builder(Component.translatable(emote.translationKey()), b -> {
                NetworkManager.sendToServer(new EmotePayloads.Play(emote.ordinal()));
                onClose();
            }).bounds(x, y, BUTTON_W, BUTTON_H).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
