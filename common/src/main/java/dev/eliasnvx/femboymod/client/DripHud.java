package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Optional small Drip Level readout (SPEC §4.6), off by default (client config {@code drip_hud}). */
public final class DripHud {

    private static final int MARGIN = 4;
    private static final int COLOR = 0xFFFF9FD0;

    private DripHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!FemboyConfig.client().dripHud() || minecraft.player == null) {
            return;
        }
        var drip = WornEvaluator.evaluate(minecraft.player).drip();
        if (drip.level() == 0) {
            return;
        }
        graphics.text(minecraft.font, Component.translatable("hud.femboymod.drip", drip.level(), drip.tier()), MARGIN, MARGIN, COLOR, true);
    }
}
