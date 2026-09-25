package dev.eliasnvx.femboymod.api.colorway;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorwayPatternTest {

    private static ColorwayPattern parse(String json) {
        return ColorwayPattern.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void fixedStripesResolveToTheirColors() {
        ColorwayPattern trans = parse("{\"stripes\":[\"#5BCEFA\",\"#f5a9b8\",\"#FFFFFF\"]}");
        assertArrayEquals(new int[]{0x5BCEFA, 0xF5A9B8, 0xFFFFFF}, trans.resolve(0, 0));
    }

    @Test
    void baseAndSecondaryUseItemColors() {
        ColorwayPattern twoTone = parse("{\"stripes\":[\"base\",\"secondary\",\"base\"]}");
        assertArrayEquals(new int[]{0x111111, 0x222222, 0x111111}, twoTone.resolve(0x111111, 0x222222));
    }

    @Test
    void roundTripKeepsFormat() {
        ColorwayPattern pattern = parse("{\"stripes\":[\"base\",\"#00ff00\"]}");
        var json = ColorwayPattern.CODEC.encodeStart(JsonOps.INSTANCE, pattern).getOrThrow();
        assertEquals("{\"stripes\":[\"base\",\"#00FF00\"]}", json.toString());
    }

    @Test
    void brokenJsonIsAnErrorNotACrash() {
        for (String bad : new String[]{
                "{\"stripes\":[]}",
                "{\"stripes\":[\"#12345\"]}",
                "{\"stripes\":[\"pink\"]}",
                "{\"stripes\":[\"#GGGGGG\"]}",
                "{}"}) {
            assertTrue(ColorwayPattern.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).isError(), bad);
        }
    }
}
