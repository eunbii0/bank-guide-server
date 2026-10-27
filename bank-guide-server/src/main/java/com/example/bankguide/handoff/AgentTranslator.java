package com.example.bankguide.handoff;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** 상담원 ↔ 고객 대화 번역. 상담원은 한국어, 고객은 자기 언어로 쓴다. */
@Component
public class AgentTranslator {

    private static final String TO_CUSTOMER_PROMPT = """
            You translate a Korean bank agent's reply into the customer's language (ISO 639-1 code given by the user).
            Keep it short, polite and easy to understand for a foreigner living in Korea.
            The app is shown to the customer in their own language, so translate button and menu names
            into that language too. Never leave Korean words in the output (unless the target language is Korean).
            Do not add anything that is not in the original. Output only the translated text.
            """;

    private static final String TO_KOREAN_PROMPT = """
            You translate a foreign customer's message to a Korean bank agent into natural Korean.
            The source language code is given by the user.
            Keep the meaning exactly. Do not add, answer or explain anything. Output only the Korean text.
            """;

    private final ChatClient chatClient;

    public AgentTranslator(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /** 상담원(한국어) → 고객 언어 */
    public String translate(String koreanText, String targetLang) {
        String translated = chatClient.prompt()
                .system(TO_CUSTOMER_PROMPT)
                .user("target language: " + targetLang + "\n\n" + koreanText)
                .call()
                .content();
        return "ko".equals(targetLang) ? translated : stripKoreanNotes(translated);
    }

    /** 번역문에 "해지 화면(Korean)"처럼 한국어 설명이 괄호로 남으면 고객이 읽을 수 없어서 괄호째 지운다 */
    static String stripKoreanNotes(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s*[(（][^()（）]*[가-힣][^()（）]*[)）]", "").trim();
    }

    /** 고객 언어 → 한국어 */
    public String toKorean(String text, String sourceLang) {
        return chatClient.prompt()
                .system(TO_KOREAN_PROMPT)
                .user("source language: " + sourceLang + "\n\n" + text)
                .call()
                .content();
    }
}
