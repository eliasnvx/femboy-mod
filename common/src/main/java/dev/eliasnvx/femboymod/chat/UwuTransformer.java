package dev.eliasnvx.femboymod.chat;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure UwU text transformation (SPEC §4.7): no Minecraft classes, unit tested. Commands and links are
 * never changed; the result is dropped (original kept) if it would exceed the chat length limit.
 */
public final class UwuTransformer {

    /** Links and @mentions are copied verbatim. */
    private static final Pattern PROTECTED = Pattern.compile("(https?://\\S+|www\\.\\S+|@\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD = Pattern.compile("[\\p{L}']+");
    private static final Pattern CYRILLIC = Pattern.compile("\\p{IsCyrillic}");

    private UwuTransformer() {
    }

    /** True for text that must not be transformed at all. */
    public static boolean isExempt(String message) {
        String trimmed = message.stripLeading();
        return trimmed.isEmpty() || trimmed.startsWith("/");
    }

    /** Chooses the rule set: Cyrillic text uses Russian rules, otherwise the client language, then English. */
    public static String pickLanguage(String message, String clientLanguage, Map<String, UwuRules> available) {
        if (CYRILLIC.matcher(message).find() && available.containsKey("ru_ru")) {
            return "ru_ru";
        }
        String lang = clientLanguage.toLowerCase(Locale.ROOT);
        if (available.containsKey(lang)) {
            return lang;
        }
        return "en_us";
    }

    /**
     * @param random    source of randomness (seed it with the message for reproducible output)
     * @param maxLength chat length limit
     */
    public static String transform(String message, UwuRules rules, Random random, int maxLength) {
        if (isExempt(message)) {
            return message;
        }
        StringBuilder out = new StringBuilder(message.length() + 8);
        Matcher link = PROTECTED.matcher(message);
        int last = 0;
        boolean first = true;
        while (link.find()) {
            first = transformText(message.substring(last, link.start()), rules, random, out, first);
            out.append(link.group());
            last = link.end();
        }
        transformText(message.substring(last), rules, random, out, first);

        String body = out.toString();
        if (!rules.suffixes().isEmpty() && random.nextFloat() < rules.suffixChance()) {
            String withSuffix = body + rules.suffixes().get(random.nextInt(rules.suffixes().size()));
            if (withSuffix.length() <= maxLength) {
                return withSuffix;
            }
        }
        return body.length() <= maxLength ? body : message;
    }

    /** @return whether the next word is still the first word of the message */
    private static boolean transformText(String text, UwuRules rules, Random random, StringBuilder out, boolean first) {
        Matcher word = WORD.matcher(text);
        int last = 0;
        while (word.find()) {
            out.append(text, last, word.start());
            String transformed = transformWord(word.group(), rules);
            if (first && transformed.length() > 1 && random.nextFloat() < rules.stutterChance()) {
                transformed = transformed.charAt(0) + "-" + transformed;
            }
            first = false;
            out.append(transformed);
            last = word.end();
        }
        out.append(text.substring(last));
        return first;
    }

    static String transformWord(String word, UwuRules rules) {
        String lower = word.toLowerCase(Locale.ROOT);
        String whole = rules.words().get(lower);
        if (whole != null) {
            return matchCase(word, whole);
        }
        String result = word;
        for (UwuRules.Replacement replacement : rules.replacements()) {
            result = replaceKeepingCase(result, replacement.from(), replacement.to());
        }
        return result;
    }

    private static String replaceKeepingCase(String text, String from, String to) {
        String lower = text.toLowerCase(Locale.ROOT);
        String needle = from.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(needle);
        if (index < 0 || needle.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length() + 4);
        int last = 0;
        while (index >= 0) {
            sb.append(text, last, index);
            sb.append(matchCase(text.substring(index, index + needle.length()), to));
            last = index + needle.length();
            index = lower.indexOf(needle, last);
        }
        sb.append(text.substring(last));
        return sb.toString();
    }

    /** Applies the case pattern of {@code original} (all caps / capitalized / lower) to {@code replacement}. */
    static String matchCase(String original, String replacement) {
        boolean hasLetter = original.chars().anyMatch(Character::isLetter);
        if (hasLetter && original.equals(original.toUpperCase(Locale.ROOT)) && original.length() > 1) {
            return replacement.toUpperCase(Locale.ROOT);
        }
        if (!original.isEmpty() && Character.isUpperCase(original.charAt(0)) && !replacement.isEmpty()) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }
}
