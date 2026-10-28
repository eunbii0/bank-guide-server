package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.MessageView;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 상담 중 고객이 화면을 옮기면 상담원 화면 대화와 현재 화면 칸에 바로 보이는지 확인한다 */
@SpringBootTest
class CustomerActivityTest {

    @Autowired
    private HandoffService handoffService;
    @Autowired
    private HandoffTicketRepository ticketRepository;

    @Test
    void 화면_이동은_대화에_한국어_화면_이름으로_쌓이고_현재_화면이_바뀐다() {
        ticket("T-ACT-1", "c-act-1");

        handoffService.customerActivity("T-ACT-1", "transfer");

        TicketView view = handoffService.ticket("T-ACT-1");
        MessageView last = view.messages().get(view.messages().size() - 1);
        assertEquals("ACTIVITY", last.sender());
        assertEquals("이체 입력", last.content());
        assertEquals("이체 입력", view.currentPage());
    }

    @Test
    void 종료된_상담에는_화면_이동을_남기지_않는다() {
        ticket("T-ACT-2", "c-act-2");
        handoffService.changeStatus("T-ACT-2", "CLOSED");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> handoffService.customerActivity("T-ACT-2", "home"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("홈", handoffService.ticket("T-ACT-2").currentPage());
    }

    private void ticket(String ticketNo, String conversationId) {
        ticketRepository.save(new HandoffTicket(ticketNo, conversationId, null, "FOREIGN", "CUSTOMER_REQUEST",
                "en", null, "home", "요약", "확인", "NONE", "RULE", null));
    }
}
