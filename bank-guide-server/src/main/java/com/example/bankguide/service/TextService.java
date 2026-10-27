package com.example.bankguide.service;

import com.example.bankguide.domain.GuideStepText;
import com.example.bankguide.domain.GuideStepTextRepository;
import com.example.bankguide.domain.GuideTaskText;
import com.example.bankguide.domain.GuideTaskTextRepository;
import com.example.bankguide.domain.UiText;
import com.example.bankguide.domain.UiTextRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 언어별 문구 조회. 요청한 언어에 문구가 없으면 영어 → 한국어 순으로 대신 쓴다.
 * 예) 네팔어로 질문한 고객에게는 영어 안내가 나간다.
 */
@Service
public class TextService {

    public static final List<String> SUPPORTED = List.of("ko", "en", "vi", "zh");
    private static final List<String> FALLBACK = List.of("en", "ko");

    private final GuideTaskTextRepository taskTextRepository;
    private final GuideStepTextRepository stepTextRepository;
    private final UiTextRepository uiTextRepository;

    public TextService(GuideTaskTextRepository taskTextRepository,
                       GuideStepTextRepository stepTextRepository,
                       UiTextRepository uiTextRepository) {
        this.taskTextRepository = taskTextRepository;
        this.stepTextRepository = stepTextRepository;
        this.uiTextRepository = uiTextRepository;
    }

    /** LLM이 알려준 언어 코드를 지원 언어로 맞춘다. "vi-VN" → "vi", "ne" → "en" */
    public String normalize(String lang) {
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        String code = lang.trim().toLowerCase().substring(0, Math.min(2, lang.trim().length()));
        return SUPPORTED.contains(code) ? code : "en";
    }

    public GuideTaskText task(String taskCode, String lang) {
        return firstFound(lang, l -> taskTextRepository.findByTaskCodeAndLang(taskCode, l))
                .orElseThrow(() -> new IllegalStateException("업무 문구가 없습니다: " + taskCode));
    }

    /** step_no → 안내 문구 */
    public Map<Integer, String> stepMessages(String taskCode, String lang) {
        for (String l : candidates(lang)) {
            List<GuideStepText> rows = stepTextRepository.findByTaskCodeAndLang(taskCode, l);
            if (!rows.isEmpty()) {
                return rows.stream().collect(Collectors.toMap(GuideStepText::getStepNo, GuideStepText::getMessage));
            }
        }
        return Map.of();
    }

    public String ui(String key, String lang) {
        return firstFound(lang, l -> uiTextRepository.findByTextKeyAndLang(key, l))
                .map(UiText::getContent)
                .orElse(key);
    }

    private <T> Optional<T> firstFound(String lang, Function<String, Optional<T>> finder) {
        for (String l : candidates(lang)) {
            Optional<T> found = finder.apply(l);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private List<String> candidates(String lang) {
        return Stream.concat(Stream.of(lang), FALLBACK.stream()).distinct().toList();
    }
}
