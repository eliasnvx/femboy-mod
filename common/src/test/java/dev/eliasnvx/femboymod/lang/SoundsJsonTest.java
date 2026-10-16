package dev.eliasnvx.femboymod.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * sounds.json: every file of ours it names is shipped and mono (Minecraft plays stereo sounds without distance
 * falloff), and every subtitle has a translation.
 */
class SoundsJsonTest {

    private static final String ASSETS = "/assets/femboymod/";
    /** Ogg Vorbis identification header: packet type 1, "vorbis", version (4 bytes), then the channel count. */
    private static final byte[] VORBIS_ID = {1, 'v', 'o', 'r', 'b', 'i', 's'};
    private static final int CHANNELS_AFTER_ID = 4;

    @Test
    void ownSoundFilesExistAndAreMono() throws IOException {
        Set<String> problems = new TreeSet<>();
        for (var event : read("sounds.json").entrySet()) {
            for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
                if (!name.startsWith("femboymod:")) {
                    continue; // vanilla sounds with our pitch and volume
                }
                String path = ASSETS + "sounds/" + name.substring("femboymod:".length()) + ".ogg";
                try (InputStream in = SoundsJsonTest.class.getResourceAsStream(path)) {
                    if (in == null) {
                        problems.add(event.getKey() + ": missing " + path);
                    } else if (channels(in.readAllBytes()) != 1) {
                        problems.add(event.getKey() + ": not mono " + path);
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "sounds.json: " + problems);
    }

    @Test
    void subtitlesAreTranslated() throws IOException {
        JsonObject english = read("lang/en_us.json");
        Set<String> missing = new TreeSet<>();
        for (var event : read("sounds.json").entrySet()) {
            JsonElement subtitle = event.getValue().getAsJsonObject().get("subtitle");
            if (subtitle != null && !english.has(subtitle.getAsString())) {
                missing.add(subtitle.getAsString());
            }
        }
        assertEquals(Set.of(), missing, "subtitles without an en_us entry (LangFilesTest covers the other languages)");
    }

    /** Channel count from the Vorbis identification header, or -1 if there is none. */
    private static int channels(byte[] ogg) {
        outer:
        for (int i = 0; i + VORBIS_ID.length + CHANNELS_AFTER_ID < ogg.length; i++) {
            for (int j = 0; j < VORBIS_ID.length; j++) {
                if (ogg[i + j] != VORBIS_ID[j]) {
                    continue outer;
                }
            }
            return ogg[i + VORBIS_ID.length + CHANNELS_AFTER_ID] & 0xFF;
        }
        return -1;
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream in = SoundsJsonTest.class.getResourceAsStream(ASSETS + file)) {
            assertNotNull(in, file);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
