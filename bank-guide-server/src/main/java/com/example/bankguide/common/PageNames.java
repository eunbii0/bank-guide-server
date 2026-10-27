package com.example.bankguide.common;

import java.util.Map;

/** 화면 ID → 상담원이 알아볼 수 있는 한국어 화면 이름 */
public final class PageNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("home", "홈"),
            Map.entry("product", "상품관리"),
            Map.entry("loan", "대출"),
            Map.entry("deposit-list", "예금 목록"),
            Map.entry("deposit-cancel", "중도해지 확인 화면"),
            Map.entry("deposit-done", "해지 신청 완료"),
            Map.entry("transfer", "이체 입력"),
            Map.entry("transfer-done", "이체 완료"),
            Map.entry("certificate", "증명서 목록"),
            Map.entry("cert-issue", "예금잔액증명서 발급"),
            Map.entry("card", "카드 메뉴"),
            Map.entry("card-check-apply", "체크카드 신청"),
            Map.entry("card-credit-apply", "신용카드 신청"),
            Map.entry("card-done", "카드 신청 완료"),
            Map.entry("join", "상품가입 메뉴"),
            Map.entry("savings-join", "적금 가입 (기간 선택)"),
            Map.entry("deposit-join", "정기예금 가입 (기간 선택)"),
            Map.entry("join-confirm", "가입 내용 확인"),
            Map.entry("join-done", "가입 신청 완료"),
            Map.entry("cert-done", "증명서 발급 완료"));

    private PageNames() {}

    /** "deposit-cancel" → "중도해지 확인 화면" (모르는 ID는 그대로) */
    public static String of(String page) {
        return page == null ? "-" : NAMES.getOrDefault(page, page);
    }
}
