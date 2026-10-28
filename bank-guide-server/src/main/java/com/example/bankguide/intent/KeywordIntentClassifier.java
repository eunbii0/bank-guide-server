package com.example.bankguide.intent;

import com.example.bankguide.domain.GuideTask;
import com.example.bankguide.domain.IntentKeyword;
import com.example.bankguide.domain.IntentKeywordRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 2단계에서 쓰던 키워드 분류기. 이제는 LLM 호출이 실패했을 때(키 없음, 네트워크 오류 등)만 쓴다.
 * 언어는 한글이 섞여 있으면 ko, 아니면 en으로 본다.
 */
@Component
public class KeywordIntentClassifier {

    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    // 이관 판단용 최소 키워드 (LLM이 없을 때의 안전망)
    private static final List<String> FRAUD_WORDS = List.of(
            "통장 빌려", "통장을 빌려", "대포통장", "안전계좌", "검찰", "경찰", "출입국",
            "lend my account", "rent my account", "safe account", "police", "prosecutor", "immigration");
    private static final List<String> ADVICE_WORDS = List.of(
            "손해", "이자가 왜", "어느 게 나아", "loss", "lose money", "why is the interest", "which is better");

    private final IntentKeywordRepository keywordRepository;

    public KeywordIntentClassifier(IntentKeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    /** tasks는 priority 순으로 정렬되어 들어온다. */
    public IntentResult classify(String text, List<GuideTask> tasks) {
        String lower = text.toLowerCase();
        String language = HANGUL.matcher(text).find() ? "ko" : "en";
        String escalation = FRAUD_WORDS.stream().anyMatch(lower::contains) ? "FRAUD"
                : ADVICE_WORDS.stream().anyMatch(lower::contains) ? "ADVICE"
                : "NONE";

        Map<String, List<String>> keywordsByTask = keywordRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        IntentKeyword::getTaskCode,
                        Collectors.mapping(IntentKeyword::getKeyword, Collectors.toList())));

        for (GuideTask task : tasks) {
            List<String> keywords = keywordsByTask.getOrDefault(task.getCode(), List.of());
            if (keywords.stream().anyMatch(lower::contains)) {
                return new IntentResult(task.getCode(), List.of(), language, escalation, "NONE");
            }
        }
        // 업무 키워드가 하나도 없으면 인사·감사·작별인지 본다 (예: "no that's all thank you")
        Optional<SmallTalk.Match> talk = SmallTalk.guess(text);
        return new IntentResult(IntentService.UNKNOWN, List.of(), talk.map(SmallTalk.Match::lang).orElse(language),
                escalation, talk.map(m -> m.kind().name()).orElse("NONE"));
    }
}
