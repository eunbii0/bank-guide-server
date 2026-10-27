package com.example.bankguide.handoff;

import java.util.List;

/**
 * 상담원용 요약. LLM에게도 이 모양의 JSON으로 답하라고 시킨다.
 *
 * @param summary     한국어 요약 3줄
 * @param checkpoints 상담원이 확인할 것 1~3개 (한국어)
 * @param riskFlag    NONE 또는 FRAUD_SUSPECTED
 */
public record SummaryResult(List<String> summary, List<String> checkpoints, String riskFlag) {}
