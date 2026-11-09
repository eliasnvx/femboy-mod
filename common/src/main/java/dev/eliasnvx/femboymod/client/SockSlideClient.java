package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.effect.SockSlide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client half of {@code femboymod:slide}: the local player moves on this client, so the friction modifier is set here,
 * at the start of every client tick (before the player moves), from the worn effects and the floor.
 */
public final class SockSlideClient {

    private SockSlideClient() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float grip = SockSlide.activeGrip(player);
        SockSlide.setGrip(player, grip < 1.0F && SockSlide.onSlipperyFloor(player) ? grip : 1.0F);
    }
}
