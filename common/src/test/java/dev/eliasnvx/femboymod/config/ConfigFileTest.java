package dev.eliasnvx.femboymod.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        // 1.20.1 registries refuse to create keys before Minecraft is bootstrapped
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @TempDir
    Path dir;

    @Test
    void missingFileWritesDefaults() {
        Path file = dir.resolve("c.json");
        ConfigFile<ClientConfig> config = new ConfigFile<>(file, ClientConfig.CODEC, ClientConfig.DEFAULTS);
        assertEquals(ClientConfig.DEFAULTS, config.load());
        assertTrue(Files.exists(file));
    }

    @Test
    void partialFileKeepsValuesAndFillsDefaults() throws Exception {
        Path file = dir.resolve("c.json");
        Files.writeString(file, "{\"nya_sound\": true, \"tail_skirt_physics\": \"simple\"}");
        ClientConfig loaded = new ConfigFile<>(file, ClientConfig.CODEC, ClientConfig.DEFAULTS).load();
        assertTrue(loaded.nyaSound());
        assertEquals(ClientConfig.Physics.SIMPLE, loaded.physics());
        assertTrue(loaded.showOthersCosmetics(), "unspecified option falls back to its default");
        assertTrue(Files.readString(file).contains("show_others_cosmetics"), "file is rewritten with all options");
    }

    @Test
    void hudCornerAutoAvoidsATopLeftMinimap() throws Exception {
        Path file = dir.resolve("c.json");
        Files.writeString(file, "{\"femboy_level_panel_corner\": \"top_right\"}");
        ClientConfig loaded = new ConfigFile<>(file, ClientConfig.CODEC, ClientConfig.DEFAULTS).load();
        assertEquals(ClientConfig.HudCorner.TOP_RIGHT, loaded.hudCorner());
        assertEquals(ClientConfig.HudCorner.TOP_RIGHT, loaded.hudCorner().resolve(true), "a chosen corner stays");
        assertEquals(ClientConfig.HudCorner.AUTO, ClientConfig.DEFAULTS.hudCorner());
        assertEquals(ClientConfig.HudCorner.TOP_LEFT, ClientConfig.HudCorner.AUTO.resolve(false));
        assertEquals(ClientConfig.HudCorner.BOTTOM_RIGHT, ClientConfig.HudCorner.AUTO.resolve(true));
    }

    @Test
    void brokenFileFallsBackAndKeepsBackup() throws Exception {
        Path file = dir.resolve("s.json");
        Files.writeString(file, "{ keep_cosmetics_on_death: yes please");
        CommonConfig loaded = new ConfigFile<>(file, CommonConfig.CODEC, CommonConfig.DEFAULTS).load();
        assertEquals(CommonConfig.DEFAULTS, loaded);
        assertTrue(Files.exists(dir.resolve("s.json.broken")));
    }

    @Test
    void outOfRangeValueIsRejected() throws Exception {
        Path file = dir.resolve("s.json");
        Files.writeString(file, "{\"pink_creeper\": {\"radius\": 999}}");
        CommonConfig loaded = new ConfigFile<>(file, CommonConfig.CODEC, CommonConfig.DEFAULTS).load();
        assertEquals(CommonConfig.PinkCreeper.DEFAULTS.radius(), loaded.pinkCreeper().radius());
        assertFalse(loaded.keepCosmeticsOnDeath());
    }
}
