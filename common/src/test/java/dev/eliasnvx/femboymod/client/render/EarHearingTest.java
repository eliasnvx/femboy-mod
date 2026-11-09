package dev.eliasnvx.femboymod.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarHearingTest {

    private static final String DIR = "/assets/femboymod/femboymod/hearing/";
    private static final List<String> BUILT_IN = List.of("cat_ears", "fox_ears", "bunny_ears", "bear_ears", "wolf_ears", "tail");

    private static DataResult<EarHearing> parse(String json) {
        return EarHearing.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    @Test
    void builtInProfilesParse() throws IOException {
        for (String name : BUILT_IN) {
            try (InputStream in = EarHearingTest.class.getResourceAsStream(DIR + name + ".json")) {
                assertNotNull(in, name);
                JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                DataResult<EarHearing> result = EarHearing.CODEC.parse(JsonOps.INSTANCE, json);
                assertTrue(result.isSuccess(), name + ": " + result.error().map(Object::toString).orElse(""));
            }
        }
    }

    @Test
    void defaultsAndNightRadius() {
        EarHearing fox = parse("{\"items\": [\"femboymod:fox_ears\"], \"radius\": 10, \"night_radius\": 20}").getOrThrow();
        assertEquals(10.0F, fox.radius(false));
        assertEquals(20.0F, fox.radius(true));
        assertEquals(1.0F, fox.flatten());
        assertFalse(fox.startles());
        // without notices_first_radius the first-noticed mobs use the normal radius
        assertEquals(20.0F, fox.firstRadius(true));
    }

    @Test
    void firstRadiusIsNeverShorterThanTheRadius() {
        EarHearing cat = parse("{\"items\": [\"#femboymod:ears\"], \"radius\": 16, \"notices_first\": [\"minecraft:creeper\"],"
                + " \"notices_first_radius\": 8}").getOrThrow();
        assertEquals(16.0F, cat.firstRadius(false));
    }

    @Test
    void brokenProfilesAreRejected() {
        assertTrue(parse("{\"radius\": 16}").isError(), "items are required");
        assertTrue(parse("{\"items\": [\"femboymod:cat_ears\"]}").isError(), "radius is required");
        assertTrue(parse("{\"items\": [\"femboymod:cat_ears\"], \"radius\": -1}").isError(), "radius must be positive");
        assertTrue(parse("{\"items\": [\"femboymod:cat_ears\"], \"radius\": 8, \"flatten\": 2}").isError(), "flatten is 0..1");
        assertTrue(parse("{\"items\": [\"Not An Id\"], \"radius\": 8}").isError(), "bad ids");
    }
}
