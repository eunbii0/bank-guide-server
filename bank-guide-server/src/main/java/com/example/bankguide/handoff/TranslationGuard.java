package com.example.bankguide.handoff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.util.Locale;

/** Deterministic loss checks, not a guarantee that the translation has the same meaning. */
final class TranslationGuard {
    private static final Pattern VALUES = Pattern.compile(
            "(?<date>\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일(?:\\s*(?:오전|오후)\\s*\\d{1,2}시(?:\\s*\\d{1,2}분)?)?)"
            + "|(?<krw>\\p{Nd}+(?:[.,]\\p{Nd}+)*\\s*원)"
            + "|[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}|\\p{Nd}+(?:[.,:/-]\\p{Nd}+)*|\\*{3,}");
    private static final Pattern KOREAN_DATE = Pattern.compile(
            "(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일(?:\\s*(오전|오후)\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?)?");
    record Prepared(String text, Map<String, String> replacements) {}

    static Prepared protect(String source, String from, String to) {
        Map<String, String> values = new LinkedHashMap<>();
        var matcher = VALUES.matcher(source);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String token = token();
            String value = matcher.group();
            if (matcher.group("date") != null && !"ko".equals(to)) value = canonicalDate(value);
            else if (matcher.group("krw") != null && !"ko".equals(to)) value = value.replaceFirst("\\s*원$", " KRW");
            values.put(token, value);
            matcher.appendReplacement(result, token);
        }
        matcher.appendTail(result);
        String protectedText = result.toString();
        for (var product : TranslationGlossary.products(from, to).entrySet()) {
            if (protectedText.contains(product.getKey())) {
                String token = token();
                values.put(token, product.getValue());
                protectedText = protectedText.replace(product.getKey(), token);
            }
        }
        return new Prepared(protectedText, values);
    }

    private static String token() { return "__KEEP_" + UUID.randomUUID().toString().replace("-", "") + "__"; }

    private static String canonicalDate(String text) {
        var matcher = KOREAN_DATE.matcher(text);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid date");
        LocalDate date = LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        if (matcher.group(4) == null) return date.toString();
        int hour = Integer.parseInt(matcher.group(5));
        int minute = matcher.group(6) == null ? 0 : Integer.parseInt(matcher.group(6));
        if (hour < 1 || hour > 12 || minute > 59) throw new IllegalArgumentException("Invalid time");
        hour = hour % 12 + ("오후".equals(matcher.group(4)) ? 12 : 0);
        return date + String.format(Locale.ROOT, " %02d:%02d", hour, minute);
    }

    static String validate(String source, Prepared input, String output, String from, String to) {
        if (output == null || output.isBlank()) throw failure("EMPTY_OUTPUT");
        String check = output;
        for (var entry : input.replacements().entrySet()) {
            // Products may occur repeatedly; every occurrence must be retained.
            if (count(output, entry.getKey()) != count(input.text(), entry.getKey())) throw failure("PROTECTED_VALUE_CHANGED");
            check = check.replace(entry.getKey(), "");
        }
        if (check.contains("__KEEP_") || check.codePoints().anyMatch(Character::isDigit)) throw failure("UNEXPECTED_VALUE");
        String sourceForLength = input.text();
        for (String token : input.replacements().keySet()) sourceForLength = sourceForLength.replace(token, "");
        long sourceLetters = sourceForLength.codePoints().filter(Character::isLetter).count();
        long translatedLetters = check.codePoints().filter(Character::isLetter).count();
        if (sourceLetters > 60 && translatedLetters < sourceLetters / 6) throw failure("SUSPICIOUS_LENGTH");
        validateScript(check, to);
        String restored = output;
        for (var entry : input.replacements().entrySet()) restored = restored.replace(entry.getKey(), entry.getValue());
        if (restored.length() > Math.max(300, source.length() * 6)) throw failure("SUSPICIOUS_LENGTH");
        if (restored.length() > 1000) throw failure("OUTPUT_TOO_LONG");
        if (TranslationGlossary.introducesCard(source, restored, from, to)) throw failure("UNSUPPORTED_CARD_TERM");
        return restored;
    }

    private static int count(String value, String part) { return (value.length() - value.replace(part, "").length()) / part.length(); }

    static void validateScript(String output, String target) {
        Character.UnicodeScript expected = switch (target) {
            case "ko" -> Character.UnicodeScript.HANGUL;
            case "zh" -> Character.UnicodeScript.HAN;
            case "ne" -> Character.UnicodeScript.DEVANAGARI;
            case "km" -> Character.UnicodeScript.KHMER;
            default -> Character.UnicodeScript.LATIN;
        };
        long letters = output.codePoints().filter(Character::isLetter).count();
        long expectedLetters = output.codePoints().filter(Character::isLetter)
                .filter(c -> Character.UnicodeScript.of(c) == expected).count();
        boolean unexpected = output.codePoints().filter(Character::isLetter).mapToObj(Character.UnicodeScript::of)
                .anyMatch(s -> s != expected && s != Character.UnicodeScript.LATIN
                        && s != Character.UnicodeScript.COMMON && s != Character.UnicodeScript.INHERITED);
        // Protected product names are restored later; ordinary prose must include the requested script.
        if (unexpected || (letters > 0 && expected != Character.UnicodeScript.LATIN && expectedLetters == 0)) {
            throw failure("TARGET_SCRIPT_MISMATCH");
        }
    }

    static IllegalStateException failure(String code) { return new IllegalStateException(code); }
}
