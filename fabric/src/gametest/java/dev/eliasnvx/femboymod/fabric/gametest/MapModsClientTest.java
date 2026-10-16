package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.FemboyMod;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Map mods smoke test, runs only with {@code -Pcompat_maps=true} (Xaero's World Map + Minimap, JourneyMap): puts every
 * block of the mod next to the player and screenshots the minimaps and the full-screen maps
 * ({@code compat_maps_*.png}). Check the log for map errors. Uses a dedicated test server, because closing a
 * singleplayer world deadlocks the test framework while Xaero's map is loaded.
 */
public final class MapModsClientTest implements FabricClientGameTest {

    private static final int COLUMNS = 8;
    private static final int SPACING = 2;
    private static final int SETTLE_TICKS = 100;
    private static final int MAP_OPEN_TICKS = 60;
    private static final int CLOSE_TICKS = 5;

    static boolean xaeroLoaded() {
        return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
    }

    private static boolean journeyMapLoaded() {
        return FabricLoader.getInstance().isModLoaded("journeymap");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!xaeroLoaded() && !journeyMapLoaded()) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("level-type", "minecraft:flat");
        List<Identifier> blocks = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals(FemboyMod.MOD_ID)).sorted().toList();
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule spawn_monsters false");
            for (int i = 0; i < blocks.size(); i++) {
                int x = 2 + (i % COLUMNS) * SPACING;
                int z = 2 + (i / COLUMNS) * SPACING;
                server.runCommand("execute as @p at @s run setblock ~" + x + " ~ ~" + z + " " + blocks.get(i));
            }
            context.waitTicks(SETTLE_TICKS);
            context.takeScreenshot("compat_maps_minimap");
            if (xaeroLoaded()) {
                openMap(context, "compat_maps_xaero", name -> name.equals("gui.xaero_open_map"));
            }
            if (journeyMapLoaded()) {
                // the first open shows JourneyMap's "what's new" screen
                openMap(context, "compat_maps_journeymap_first_run", name -> name.equals("key.journeymap.map_toggle_alt"));
                openMap(context, "compat_maps_journeymap", name -> name.equals("key.journeymap.map_toggle_alt"));
            }
        }
    }

    private static void openMap(ClientGameTestContext context, String shot, Predicate<String> keyName) {
        context.getInput().pressKey(options -> Arrays.stream(options.keyMappings)
                .filter(key -> keyName.test(key.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("no map key among " + Arrays.stream(options.keyMappings)
                        .map(KeyMapping::getName).filter(n -> n.contains("map")).toList())));
        context.waitTicks(MAP_OPEN_TICKS);
        context.takeScreenshot(shot);
        context.runOnClient(mc -> mc.gui.setScreen(null));
        context.waitTicks(CLOSE_TICKS);
    }
}
