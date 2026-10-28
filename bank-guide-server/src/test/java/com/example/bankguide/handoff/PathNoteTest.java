package com.example.bankguide.handoff;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathNoteTest {

    private static final Function<String, String> TASKS =
            code -> Map.of("CHECK_CARD", "체크카드 발급", "SAVINGS_OPEN", "적금 가입").getOrDefault(code, code);

    private static String ko(String type, String detail) {
        return PathNote.detailKo(type, detail, TASKS);
    }

    @Test
    void 안내_시작은_업무_기간_언어로_보여준다() {
        assertEquals("체크카드 발급 · 영어", ko("GUIDE_START", "CHECK_CARD (en)"));
        assertEquals("적금 가입 · 6개월 · 베트남어", ko("GUIDE_START", "SAVINGS_OPEN 6개월 (vi)"));
    }

    @Test
    void 화면_진입은_단계_이탈_완료로_보여준다() {
        assertEquals("2/4단계 안내", ko("PAGE_ENTER", "STEP 2/4"));
        assertEquals("경로 이탈", ko("PAGE_ENTER", "OFF_PATH"));
        assertEquals("목표 화면 도착 (안내 완료)", ko("PAGE_ENTER", "DONE"));
    }

    @Test
    void 연결_제안_멈춤_상담원_연결() {
        assertEquals("경로 이탈 3회 → 상담원 연결 제안", ko("OFFER", "OFF_PATH x3"));
        assertEquals("30초 동안 멈춤", ko("STUCK", "idle 30s"));
        assertEquals("사유: 경로 이탈 반복", ko("HANDOFF", "OFF_PATH"));
        assertEquals("사유: 고객이 직접 요청", ko("HANDOFF", "CUSTOMER_REQUEST"));
    }

    @Test
    void 내용이_없거나_모르는_형식이면_그대로() {
        assertEquals("-", ko("GUIDE_STOP", null));
        assertEquals("뭔가 새 형식", ko("PAGE_ENTER", "뭔가 새 형식"));
        assertEquals("NEW_TASK · 영어", ko("GUIDE_START", "NEW_TASK (en)")); // 모르는 업무 코드는 코드 그대로
    }
}
