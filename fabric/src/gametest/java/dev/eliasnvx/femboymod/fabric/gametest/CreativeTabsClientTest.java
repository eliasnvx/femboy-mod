package dev.eliasnvx.femboymod.fabric.gametest;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The creative inventory open on each femboymod tab. Skipped unless {@code FEMBOYMOD_TAB_SHOTS=1}:
 * {@code FEMBOYMOD_TAB_SHOTS=1 ./gradlew :fabric:runClientGameTest}; files: {@code creative_tab_*.png}.
 */
public final class CreativeTabsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 5;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (System.getenv("FEMBOYMOD_TAB_SHOTS") == null) {
            return;
        }
        List<RegistrySupplier<CreativeModeTab>> tabs = List.of(FemboyItems.TAB, FemboyItems.ROOM_TAB, FemboyItems.SNACKS_TAB,
                FemboyItems.MATERIALS_TAB, FemboyItems.MOBS_TAB);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            context.waitTicks(SETTLE);
            context.setScreen(() -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                return new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(),
                        mc.options.operatorItemsTab().get());
            });
            for (RegistrySupplier<CreativeModeTab> tab : tabs) {
                context.runOnClient(mc -> ((FabricCreativeModeInventoryScreen) mc.gui.screen()).setSelectedTab(tab.get()));
                context.waitTicks(SETTLE);
                context.takeScreenshot(TestScreenshotOptions.of("creative_tab_" + tab.getId().getPath()));
            }
            context.setScreen(() -> null);
        }
    }
}
