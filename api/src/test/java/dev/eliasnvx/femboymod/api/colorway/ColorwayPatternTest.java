package dev.eliasnvx.femboymod.api.colorway;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorwayPatternTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        // 1.20.1 registries refuse to create keys before Minecraft is bootstrapped
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static ColorwayPattern parse(String json) {
        return ColorwayPattern.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(false, error -> { });
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
        var json = ColorwayPattern.CODEC.encodeStart(JsonOps.INSTANCE, pattern).getOrThrow(false, error -> { });
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
            assertTrue(ColorwayPattern.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error().isPresent(), bad);
        }
    }

    @Test
    void chevronCoversLeftEdgeAndStripesTheRest() {
        ColorwayPattern progress = parse("{\"stripes\":[\"#FF0000\",\"#0000FF\"],"
                + "\"chevron\":{\"colors\":[\"#FFFFFF\",\"#000000\"],\"band_width\":0.1}}");
        assertEquals(0xFFFFFF, progress.colorAt(0.02F, 0.5F, 0, 0), "left edge, middle: first chevron band");
        assertEquals(0x000000, progress.colorAt(0.15F, 0.5F, 0, 0), "second chevron band");
        assertEquals(0xFF0000, progress.colorAt(0.9F, 0.2F, 0, 0), "right side top: first stripe");
        assertEquals(0x0000FF, progress.colorAt(0.9F, 0.8F, 0, 0), "right side bottom: second stripe");
        // chevron is an arrow: at the top edge it is narrower than in the middle
        assertEquals(0xFF0000, progress.colorAt(0.15F, 0.05F, 0, 0), "outside the arrow near the top");
    }

    @Test
    void chevronIsOptional() {
        assertTrue(parse("{\"stripes\":[\"base\"]}").chevron().isEmpty());
    }

    @Test
    void shimmerFlowsSmoothlyAndLoops() {
        ColorwayPattern glitter = parse("{\"stripes\":[\"#FF0000\",\"#0000FF\"],\"shimmer\":{\"period_ticks\":100}}");
        assertEquals(0xFF0000, glitter.stripeColor(0, 0.0F, 0, 0), "resting color at t=0");
        assertEquals(0x0000FF, glitter.stripeColor(0, 50.0F, 0, 0), "half a period: moved one stripe on (2 stripes)");
        assertEquals(0xFF0000, glitter.stripeColor(0, 100.0F, 0, 0), "full period loops back");
        int previous = glitter.stripeColor(0, 0.0F, 0, 0);
        for (int tick = 1; tick <= 200; tick++) {
            int color = glitter.stripeColor(0, tick * 0.5F, 0, 0);
            int step = Math.abs(((color >> 16) & 0xFF) - ((previous >> 16) & 0xFF)) + Math.abs((color & 0xFF) - (previous & 0xFF));
            assertTrue(step <= 40, "no sudden jump at half-tick " + tick + ": " + step);
            previous = color;
        }
    }

    @Test
    void withoutShimmerTimeIsIgnored() {
        ColorwayPattern plain = parse("{\"stripes\":[\"#FF0000\",\"#0000FF\"]}");
        assertTrue(plain.shimmer().isEmpty());
        assertEquals(0x0000FF, plain.stripeColor(1, 1234.0F, 0, 0));
    }

    @Test
    void shimmerCannotStrobe() {
        // 1.20.1 DFU: optionalFieldOf drops an invalid value instead of failing, so a too-fast shimmer is either
        // rejected or removed; it can never come out as a flashing pattern
        var result = ColorwayPattern.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"stripes\":[\"#FF0000\"],\"shimmer\":{\"period_ticks\":" + (ColorwayPattern.Shimmer.MIN_PERIOD_TICKS - 1) + "}}"));
        assertTrue(result.error().isPresent() || result.result().orElseThrow().shimmer().isEmpty());
    }
}
