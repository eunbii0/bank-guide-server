package com.example.bankguide.eligibility;

import com.example.bankguide.api.ApiDtos.EligibilityRequest;
import com.example.bankguide.api.ApiDtos.EligibilityResponse;
import com.example.bankguide.domain.ChatMessage;
import com.example.bankguide.domain.EligibilityCheck;
import com.example.bankguide.domain.EligibilityCheckRepository;
import com.example.bankguide.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 서버를 띄워 data.sql의 실제 규칙·문구로 확인한다 (기준일 2026-10-27 고정).
 * 상담원 연결 쪽에 넘길 eligibility_check 저장 내용과, 채팅 기록에 개인정보가 남지 않는지를 본다.
 */
@SpringBootTest(properties = "demo.today=2026-10-27")
class EligibilityServiceTest {

    @Autowired
    private EligibilityService eligibilityService;
    @Autowired
    private EligibilityCheckRepository checkRepository;
    @Autowired
    private ConversationService conversationService;

    @Test
    void 고객A_카드_불가_상품이_있으면_연결_제안과_NOT_ELIGIBLE을_저장한다() {
        EligibilityResponse res = eligibilityService.check(new EligibilityRequest("t-A", "A", "CARD", "ko", true));

        assertEquals("CHECKED", res.status());
        assertNotNull(res.offer());
        EligibilityCheck saved = only("t-A");
        assertEquals("A", saved.getCustomerId());
        assertEquals("NOT_ELIGIBLE", saved.getHandoffReason());
        assertTrue(saved.getResultJson().contains("\"productCode\":\"CREDIT_CARD\",\"intent\":\"CREDIT_CARD\",\"eligible\":false,\"reasonKeys\":[\"RULE_VISA\"]"));
        assertTrue(saved.getResultKo().contains("신용카드 발급: 불가"));
    }

    @Test
    void 고객B_모두_가능하면_연결_제안이_없다() {
        EligibilityResponse res = eligibilityService.check(new EligibilityRequest("t-B", "B", "JOIN", "zh", true));

        assertNull(res.offer());
        assertNull(only("t-B").getHandoffReason());
    }

    @Test
    void 고객C_아니요면_갱신_안내와_연결_제안_PROFILE_MISMATCH를_저장한다() {
        EligibilityResponse res = eligibilityService.check(new EligibilityRequest("t-C", "C", "CARD", "en", false));

        assertEquals("MISMATCH", res.status());
        assertEquals("Shall I connect you to an agent?", res.offer());
        EligibilityCheck saved = only("t-C");
        assertFalse(saved.isConfirmed());
        assertEquals("PROFILE_MISMATCH", saved.getHandoffReason());
    }

    @Test
    void 채팅_기록에는_체류자격과_만료일이_남지_않는다() {
        eligibilityService.check(new EligibilityRequest("t-PII", "A", "JOIN", "vi", true));

        List<String> said = conversationService.history("t-PII").stream().map(ChatMessage::getContent).toList();
        assertFalse(said.isEmpty());
        assertTrue(said.stream().noneMatch(s -> s.contains("E-9") || s.contains("2027")), said.toString());
    }

    private EligibilityCheck only(String conversationId) {
        List<EligibilityCheck> rows = checkRepository.findByConversationIdOrderByIdAsc(conversationId);
        assertEquals(1, rows.size());
        return rows.get(0);
    }
}
