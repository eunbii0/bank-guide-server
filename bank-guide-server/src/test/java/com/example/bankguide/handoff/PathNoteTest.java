package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.TimelineView;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void 업무_진행_이력은_챗봇_시작부터_이모지와_한국어_문장으로_만든다() {
        List<TimelineView> t = PathNote.timeline("14:01:50", List.of(
                new EventView("14:02:00", "GUIDE_START", "home", "CHECK_CARD (en)"),
                new EventView("14:02:05", "GUIDE_STOP", null, null),
                new EventView("14:02:11", "GUIDE_START", "home", "SAVINGS_OPEN 6개월 (en)"),
                new EventView("14:02:18", "PAGE_ENTER", "join", "STEP 2/4"),
                new EventView("14:02:31", "PAGE_ENTER", "loan", "OFF_PATH"),
                new EventView("14:02:40", "STUCK", "savings-join", "idle 30s"),
                new EventView("14:02:45", "HANDOFF", null, "CUSTOMER_REQUEST")), TASKS);

        assertEquals(new TimelineView("14:01:50", "info", "💬", "챗봇 상담 시작"), t.get(0));
        assertEquals("안내 시작 · 체크카드 발급 · 영어", t.get(1).text());
        assertEquals("고객이 안내를 그만둠", t.get(2).text());
        assertEquals(new TimelineView("14:02:18", "ok", "✅", "상품가입 메뉴 진입 · 2/4단계"), t.get(4));
        assertEquals(new TimelineView("14:02:31", "warn", "⚠️", "대출 진입 · 안내 경로에서 벗어남"), t.get(5));
        assertEquals(new TimelineView("14:02:40", "warn", "⏸️", "적금 가입 (기간 선택)에서 30초 동안 멈춤"), t.get(6));
        assertEquals(new TimelineView("14:02:45", "agent", "🎧", "상담원 연결 요청 · 고객이 직접 요청"), t.get(7));
    }

    @Test
    void 채팅이_없으면_챗봇_시작_줄을_넣지_않는다() {
        assertEquals(List.of(), PathNote.timeline(null, List.of(), TASKS));
    }

    @Test
    void 내용이_없거나_모르는_형식이면_그대로() {
        assertEquals("-", ko("GUIDE_STOP", null));
        assertEquals("뭔가 새 형식", ko("PAGE_ENTER", "뭔가 새 형식"));
        assertEquals("NEW_TASK · 영어", ko("GUIDE_START", "NEW_TASK (en)")); // 모르는 업무 코드는 코드 그대로
    }
}
