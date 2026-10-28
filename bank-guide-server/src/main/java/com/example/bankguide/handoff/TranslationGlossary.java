package com.example.bankguide.handoff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Snapshot of app PAGE labels. Generic terms are hints; official product names are protected. */
final class TranslationGlossary {
    record Entry(String key, Map<String, String> labels) {}
    record Hint(String source, String target) {}
    private static final List<Entry> ENTRIES = load();

    static List<Hint> hints(String source, String from, String to) {
        return ENTRIES.stream().filter(e -> contains(source, e.labels().get(from)))
                .map(e -> new Hint(e.labels().get(from), e.labels().get(to))).toList();
    }

    static Map<String, String> products(String from, String to) {
        return ENTRIES.stream().filter(e -> e.key().startsWith("cardName"))
                .collect(java.util.stream.Collectors.toMap(e -> e.labels().get(from), e -> e.labels().get(to)));
    }

    static boolean introducesCard(String source, String translated, String from, String to) {
        String deposit = label("deposit", from);
        boolean depositSource = contains(source, deposit) || ("km".equals(from) && source.contains("ប្រាក់បញ្ញើ"));
        return depositSource && !contains(source, cardStem(from)) && contains(translated, cardStem(to));
    }

    private static String cardStem(String lang) {
        return switch (lang) {
            case "ko" -> "카드"; case "en" -> "card"; case "vi" -> "thẻ";
            case "zh" -> "卡"; case "uz" -> "karta"; case "ne" -> "कार्ड"; case "km" -> "កាត";
            default -> throw new IllegalArgumentException("Unsupported language");
        };
    }

    private static String label(String key, String lang) {
        return ENTRIES.stream().filter(e -> e.key().equals(key)).findFirst().orElseThrow().labels().get(lang);
    }

    private static boolean contains(String text, String term) {
        if (term == null || term.isBlank()) return false;
        return text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    private static List<Entry> load() {
        try (var stream = TranslationGlossary.class.getResourceAsStream("/translation-glossary.json")) {
            if (stream == null) throw new IllegalStateException("Translation glossary missing");
            return new ObjectMapper().readValue(stream, new TypeReference<List<Entry>>() {});
        } catch (IOException e) { throw new IllegalStateException("Cannot load translation glossary", e); }
    }
}
