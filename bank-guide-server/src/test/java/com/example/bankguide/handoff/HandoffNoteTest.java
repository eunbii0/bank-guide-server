package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.MessageView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandoffNoteTest {

    private final HandoffNote.Input input = new HandoffNote.Input("vi", "예금 중도해지", "ADVICE", "deposit-cancel",
            List.of(
                    new EventView("19:02:09", "GUIDE_START", "home", "DEPOSIT_CANCEL (vi)"),
                    new EventView("19:02:24", "PAGE_ENTER", "loan", "OFF_PATH"),
                    new EventView("19:03:27", "STUCK", "deposit-cancel", "idle 30s")),
            List.of(
                    new MessageView("19:02:05", "CUSTOMER", "vi", "Tôi muốn rút tiền gửi có kỳ hạn ra trước hạn"),
                    new MessageView("19:03:30", "CUSTOMER", "vi", "Sao tiền lãi ít vậy? Tài khoản 110-234-567890")));

    @Test
    void 규칙_요약은_업무_화면_원문을_한국어로_정리한다() {
        SummaryResult r = HandoffNote.fallback(input);

        assertEquals(3, r.summary().size());
        assertEquals("예금 중도해지 진행 중 상담원 연결 (사유: 손익·상품 판단 질문).", r.summary().get(0));
        assertEquals("마지막 화면: 중도해지 확인 화면, 경로 이탈 1회, 멈춤 1회.", r.summary().get(1));
        assertTrue(r.summary().get(2).contains("베트남어 원문"));
        assertTrue(r.summary().get(2).contains("***"), "계좌번호는 가려야 한다");
        assertEquals("NONE", r.riskFlag());
    }

    @Test
    void 사기_의심이면_위험_표시를_유지한다() {
        SummaryResult llm = new SummaryResult(List.of("요약"), List.of(), "NONE");
        assertEquals("FRAUD_SUSPECTED", HandoffNote.sanitize(llm, "FRAUD").riskFlag());
    }

    @Test
    void 카드_발급_같은_평범한_상담에는_제3자_확인_항목을_빼고_이체와_해지_사기에는_남긴다() {
        SummaryResult llm = new SummaryResult(List.of("요약"),
                List.of("영문 이름 확인", "제3자가 계좌 이동을 요구했는지 확인"), "NONE");

        assertEquals(List.of("영문 이름 확인"), HandoffNote.sanitize(llm, "STUCK", "체크카드 발급").checkpoints());
        assertEquals(2, HandoffNote.sanitize(llm, "ADVICE", "예금 중도해지").checkpoints().size());
        assertEquals(2, HandoffNote.sanitize(llm, "CUSTOMER_REQUEST", "송금").checkpoints().size());
        assertEquals(2, HandoffNote.sanitize(llm, "FRAUD", "체크카드 발급").checkpoints().size());
    }

    @Test
    void 비어_있는_LLM_요약은_쓰지_않는다() {
        assertNull(HandoffNote.sanitize(new SummaryResult(List.of(), List.of(), "NONE"), "ADVICE"));
        assertNull(HandoffNote.sanitize(null, "ADVICE"));
    }

    @Test
    void LLM_입력에는_한국어_화면_이름과_가린_원문이_들어간다() {
        String prompt = HandoffNote.toPrompt(input);

        assertTrue(prompt.contains("고객 언어: 베트남어"));
        assertTrue(prompt.contains("현재 화면: 중도해지 확인 화면"));
        assertTrue(prompt.contains("Tài khoản ***"));
    }
}
