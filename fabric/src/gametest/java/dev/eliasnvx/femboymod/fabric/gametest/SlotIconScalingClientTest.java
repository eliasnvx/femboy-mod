package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.client.gui.FemboyGui;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the empty cosmetic slots with the real slot code at several GUI scales and screenshots them, to check that the
 * slot icons stay on the GUI's pixel grid. Skipped unless {@code FEMBOYMOD_ICON_SHOTS=1}:
 * {@code FEMBOYMOD_ICON_SHOTS=1 ./gradlew :fabric:runClientGameTest}; files: {@code slotscale_<scale>.png}.
 * The slots sit at {@link #ORIGIN} + column/row * {@link #STEP} in GUI units, five per row.
 */
public final class SlotIconScalingClientTest implements FabricClientGameTest {

    public static final int ORIGIN = 4;
    public static final int STEP = 20;
    public static final int PER_ROW = 5;
    private static final int[] SCALES = {1, 2, 3, 4, 5, 6};
    private static final int SETTLE = 6;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_ICON_SHOTS") == null) {
            return;
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            for (int scale : SCALES) {
                context.runOnClient(mc -> mc.getWindow().setGuiScale(scale));
                context.setScreen(SlotScreen::new);
                context.waitTicks(SETTLE);
                context.takeScreenshot("slotscale_" + scale);
                context.runOnClient(mc -> mc.gui.setScreen(null));
            }
        }
    }

    private static final class SlotScreen extends Screen {

        SlotScreen() {
            super(Component.empty());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFF202020);
            List<Identifier> slots = CosmeticsManager.orderedSlots();
            for (int i = 0; i < slots.size(); i++) {
                int x = ORIGIN + (i % PER_ROW) * STEP;
                int y = ORIGIN + (i / PER_ROW) * STEP;
                FemboyGui.cosmeticSlot(g, Minecraft.getInstance().font, slots.get(i), ItemStack.EMPTY, x, y, false, false);
            }
        }
    }
}
