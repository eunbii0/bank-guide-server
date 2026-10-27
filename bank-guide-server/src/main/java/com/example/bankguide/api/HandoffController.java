package com.example.bankguide.api;

import com.example.bankguide.api.ApiDtos.HandoffRequest;
import com.example.bankguide.api.ApiDtos.HandoffResponse;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.handoff.HandoffService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HandoffController {

    private final HandoffService handoffService;

    public HandoffController(HandoffService handoffService) {
        this.handoffService = handoffService;
    }

    /** 상담원 연결: 한국어 요약을 만들어 티켓을 대기열에 넣는다 */
    @PostMapping("/handoff")
    public HandoffResponse handoff(@RequestBody HandoffRequest request) {
        return handoffService.handoff(request);
    }

    /** 대기 중인 티켓 목록 (최신순) — 5단계 상담원 화면에서 사용 */
    @GetMapping("/tickets")
    public List<TicketView> tickets() {
        return handoffService.tickets();
    }

    /** 티켓 하나: 한국어 요약 + 확인할 것 + 원문 대화 */
    @GetMapping("/tickets/{ticketNo}")
    public TicketView ticket(@PathVariable String ticketNo) {
        return handoffService.ticket(ticketNo);
    }
}
