package com.example.bankguide.handoff;

import com.example.bankguide.common.Masking;
import com.example.bankguide.domain.ChatMessage;
import java.util.List;

/** Bounded reference data; never an instruction or proof of the customer's current intent. */
public record TranslationContext(String taskCode, List<Turn> recentTurns) {
    public record Turn(String speaker, String text) {}
    public static TranslationContext empty() { return new TranslationContext(null, List.of()); }

    public static TranslationContext from(String taskCode, List<ChatMessage> history) {
        List<Turn> turns = history.stream()
                .filter(m -> "CUSTOMER".equals(m.getSender()) || "AGENT".equals(m.getSender()))
                .map(m -> new Turn(m.getSender(), reference(m.getContent())))
                .toList();
        return new TranslationContext(taskCode, turns.subList(Math.max(0, turns.size() - 4), turns.size()));
    }

    static String reference(String text) {
        String masked = Masking.mask(text).replaceAll("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "[email]");
        return masked.length() > 300 ? masked.substring(0, 300) : masked;
    }
}
