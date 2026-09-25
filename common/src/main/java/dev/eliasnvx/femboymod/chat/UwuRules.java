package dev.eliasnvx.femboymod.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Rules of the UwU chat transformer for one language, loaded from resource packs at
 * {@code assets/<ns>/femboymod/chat_transform/<language>.json} (e.g. {@code ru_ru.json}). SFW only.
 *
 * @param replacements     literal letter/syllable replacements applied inside words, in order (case preserved)
 * @param words            whole-word replacements, checked first (case preserved)
 * @param stutterChance    chance to stutter the first word ("h-hello")
 * @param suffixChance     chance to append one of {@code suffixes}
 * @param suffixes         endings like " uwu"
 */
public record UwuRules(List<Replacement> replacements, Map<String, String> words, float stutterChance,
                       float suffixChance, List<String> suffixes) {

    public static final Codec<UwuRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Replacement.CODEC.listOf().optionalFieldOf("replacements", List.of()).forGetter(UwuRules::replacements),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("words", Map.of()).forGetter(UwuRules::words),
            Codec.floatRange(0, 1).optionalFieldOf("stutter_chance", 0.0F).forGetter(UwuRules::stutterChance),
            Codec.floatRange(0, 1).optionalFieldOf("suffix_chance", 0.0F).forGetter(UwuRules::suffixChance),
            Codec.STRING.listOf().optionalFieldOf("suffixes", List.of()).forGetter(UwuRules::suffixes)
    ).apply(i, UwuRules::new));

    public UwuRules {
        replacements = List.copyOf(replacements);
        words = Map.copyOf(words);
        suffixes = List.copyOf(suffixes);
    }

    public record Replacement(String from, String to) {
        public static final Codec<Replacement> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("from").forGetter(Replacement::from),
                Codec.STRING.fieldOf("to").forGetter(Replacement::to)
        ).apply(i, Replacement::new));
    }
}
