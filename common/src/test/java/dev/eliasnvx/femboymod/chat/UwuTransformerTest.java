package dev.eliasnvx.femboymod.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UwuTransformerTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        // 1.20.1 registries refuse to create keys before Minecraft is bootstrapped
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final UwuRules EN = new UwuRules(
            List.of(new UwuRules.Replacement("r", "w"), new UwuRules.Replacement("l", "w")),
            Map.of("hello", "hewwo"), 0.0F, 0.0F, List.of());
    private static final UwuRules RU = new UwuRules(
            List.of(new UwuRules.Replacement("р", "в")), Map.of(), 0.0F, 1.0F, List.of(" uwu"));

    private static String run(String message, UwuRules rules) {
        return UwuTransformer.transform(message, rules, new Random(42), 256);
    }

    @Test
    void specExampleRussian() {
        assertEquals("пвивет uwu", run("привет", RU));
    }

    @Test
    void replacesLettersAndWordsKeepingCase() {
        assertEquals("Hewwo fwiend", run("Hello friend", EN));
        assertEquals("HEWWO", run("HELLO", EN));
    }

    @Test
    void commandsAreNeverTransformed() {
        assertEquals("/tell Steve hello", run("/tell Steve hello", EN));
        assertEquals("  /msg friend", run("  /msg friend", EN));
    }

    @Test
    void linksAndMentionsAreKeptVerbatim() {
        assertEquals("wook https://example.org/real?lol=1 @Rory", run("look https://example.org/real?lol=1 @Rory", EN));
    }

    @Test
    void falsBackToOriginalWhenTooLong() {
        String longMessage = "r".repeat(256);
        UwuRules growing = new UwuRules(List.of(new UwuRules.Replacement("r", "ww")), Map.of(), 0, 0, List.of());
        assertEquals(longMessage, UwuTransformer.transform(longMessage, growing, new Random(1), 256));
    }

    @Test
    void stutterOnlyTheFirstWord() {
        UwuRules stutter = new UwuRules(List.of(), Map.of(), 1.0F, 0.0F, List.of());
        assertEquals("h-hi there", run("hi there", stutter));
    }

    @Test
    void cyrillicPicksRussianRules() {
        Map<String, UwuRules> rules = Map.of("en_us", EN, "ru_ru", RU, "es_es", EN);
        assertEquals("ru_ru", UwuTransformer.pickLanguage("привет all", "en_us", rules));
        assertEquals("es_es", UwuTransformer.pickLanguage("hola", "ES_ES", rules));
        assertEquals("en_us", UwuTransformer.pickLanguage("hi", "de_de", rules));
        assertTrue(UwuTransformer.isExempt(""));
    }
}
