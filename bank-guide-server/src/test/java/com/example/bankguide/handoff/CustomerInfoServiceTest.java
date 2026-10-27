package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.CustomerInfoView;
import com.example.bankguide.api.ApiDtos.EligibilityRequest;
import com.example.bankguide.domain.CustomerInfoAccessRepository;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import com.example.bankguide.eligibility.EligibilityService;
import com.example.bankguide.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 상담 중 동의: 고객이 허용하기 전·거절한 뒤·상담이 끝난 뒤에는 고객 정보를 볼 수 없고,
 * 볼 때마다 조회 기록이 남는지 확인한다. 티켓은 LLM 요약 없이 저장소에 바로 만든다.
 */
@SpringBootTest(properties = "demo.today=2026-10-27")
class CustomerInfoServiceTest {

    @Autowired
    private CustomerInfoService infoService;
    @Autowired
    private HandoffTicketRepository ticketRepository;
    @Autowired
    private CustomerInfoAccessRepository accessRepository;
    @Autowired
    private EligibilityService eligibilityService;
    @Autowired
    private ConversationService conversationService;

    @Test
    void 허용하기_전에는_볼_수_없다() {
        ticket("T-INFO-1", "c-info-1", "A");

        assertForbidden("T-INFO-1");
        infoService.request("T-INFO-1");
        assertForbidden("T-INFO-1");
        assertEquals(0, accessRepository.findByTicketNoOrderByIdAsc("T-INFO-1").size());
    }

    @Test
    void 허용하면_고객_정보와_자격_판단_결과를_보고_조회할_때마다_기록이_남는다() {
        eligibilityService.check(new EligibilityRequest("c-info-2", "A", "CARD", "vi", true));
        ticket("T-INFO-2", "c-info-2", "A");

        infoService.request("T-INFO-2");
        assertEquals(CustomerInfoService.GRANTED, infoService.answer("T-INFO-2", true).infoConsent());

        CustomerInfoView first = infoService.info("T-INFO-2", "agent-kim");
        assertEquals("A", first.customerId());
        assertEquals(1, first.eligibility().size());
        assertTrue(first.eligibility().get(0).result().stream().anyMatch(l -> l.contains("신용카드 발급: 불가")));
        assertEquals(1, first.accessLog().size());
        assertEquals("agent-kim", first.accessLog().get(0).agentId());

        assertEquals(2, infoService.info("T-INFO-2", null).accessLog().size());
    }

    @Test
    void 거절하면_볼_수_없고_다시_요청할_수_있다() {
        ticket("T-INFO-3", "c-info-3", "B");

        infoService.request("T-INFO-3");
        assertEquals(CustomerInfoService.DENIED, infoService.answer("T-INFO-3", false).infoConsent());
        assertForbidden("T-INFO-3");
        assertEquals(CustomerInfoService.REQUESTED, infoService.request("T-INFO-3").infoConsent());
    }

    @Test
    void 상담이_끝나면_허용했어도_볼_수_없다() {
        HandoffTicket t = ticket("T-INFO-4", "c-info-4", "C");
        infoService.request("T-INFO-4");
        infoService.answer("T-INFO-4", true);

        t = ticketRepository.findByTicketNo("T-INFO-4").orElseThrow();
        t.setStatus("CLOSED");
        ticketRepository.save(t);
        assertForbidden("T-INFO-4");
    }

    @Test
    void 고객이_없는_티켓은_요청할_수_없다() {
        ticket("T-INFO-5", "c-info-5", null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> infoService.request("T-INFO-5"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void 채팅_기록에는_고객_정보가_남지_않는다() {
        ticket("T-INFO-6", "c-info-6", "A");
        infoService.request("T-INFO-6");
        infoService.answer("T-INFO-6", true);
        infoService.info("T-INFO-6", null);

        // 채팅 기록은 LLM 요약·답변 추천의 재료라서 체류자격이 들어가면 안 된다
        assertFalse(conversationService.history("c-info-6").stream().anyMatch(m -> m.getContent().contains("E-9")));
    }

    private HandoffTicket ticket(String ticketNo, String conversationId, String customerId) {
        return ticketRepository.save(new HandoffTicket(ticketNo, conversationId, null, "FOREIGN", "CUSTOMER_REQUEST",
                "vi", null, "home", "요약", "", "NONE", "RULE", customerId));
    }

    private void assertForbidden(String ticketNo) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> infoService.info(ticketNo, null));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
}
