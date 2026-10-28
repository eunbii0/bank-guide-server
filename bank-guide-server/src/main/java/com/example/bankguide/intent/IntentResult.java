package com.example.bankguide.intent;

import java.util.List;

/**
 * 분류기(LLM 또는 키워드)가 돌려주는 결과.
 * LLM에게도 정확히 이 모양의 JSON으로 답하라고 시킨다.
 *
 * @param intent     업무 코드 하나 (확실할 때), AMBIGUOUS, 또는 UNKNOWN
 * @param candidates 후보가 둘 이상으로 애매할 때 그 코드들
 * @param language   고객이 쓴 언어 (ISO 639-1, 예: ko, en, vi, zh)
 * @param escalation 사람에게 넘겨야 하는지 (4단계): NONE, ADVICE(손익·상품 판단), FRAUD(금융사기 의심)
 * @param talk       업무가 아닌 말: GREETING(인사), THANKS(감사), GOODBYE(더 필요 없음·작별), 아니면 NONE.
 *                   intent가 UNKNOWN일 때만 의미가 있고, 비어 있으면 NONE으로 본다
 */
public record IntentResult(String intent, List<String> candidates, String language, String escalation, String talk) {}
