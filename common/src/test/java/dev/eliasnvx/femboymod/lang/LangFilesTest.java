package dev.eliasnvx.femboymod.lang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every translation has exactly the keys of en_us and no empty values (release checklist). */
class LangFilesTest {

    @ParameterizedTest
    @ValueSource(strings = {"ru_ru", "es_es"})
    void sameKeysAsEnglish(String lang) throws IOException {
        JsonObject english = read("en_us");
        JsonObject other = read(lang);
        assertEquals(new TreeSet<>(english.keySet()), new TreeSet<>(other.keySet()), lang + " keys differ from en_us");
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_us", "ru_ru", "es_es"})
    void noEmptyValues(String lang) throws IOException {
        JsonObject json = read(lang);
        Set<String> empty = new TreeSet<>();
        json.entrySet().forEach(entry -> {
            if (entry.getValue().getAsString().isBlank()) {
                empty.add(entry.getKey());
            }
        });
        assertTrue(empty.isEmpty(), lang + " has empty values: " + empty);
    }

    private static JsonObject read(String lang) throws IOException {
        String path = "/assets/femboymod/lang/" + lang + ".json";
        try (InputStream in = LangFilesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
