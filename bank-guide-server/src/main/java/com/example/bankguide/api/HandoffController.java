package com.example.bankguide.api;

import com.example.bankguide.api.ApiDtos.AgentPoll;
import com.example.bankguide.api.ApiDtos.CustomerMessageRequest;
import com.example.bankguide.api.ApiDtos.HandoffRequest;
import com.example.bankguide.api.ApiDtos.HandoffResponse;
import com.example.bankguide.api.ApiDtos.MessageView;
import com.example.bankguide.api.ApiDtos.ReplyRequest;
import com.example.bankguide.api.ApiDtos.StatusRequest;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.handoff.HandoffService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 대기 중인 티켓 목록 (최신순) — 상담원 화면에서 사용 */
    @GetMapping("/tickets")
    public List<TicketView> tickets() {
        return handoffService.tickets();
    }

    /** 티켓 하나: 한국어 요약 + 확인할 것 + 원문 대화 */
    @GetMapping("/tickets/{ticketNo}")
    public TicketView ticket(@PathVariable String ticketNo) {
        return handoffService.ticket(ticketNo);
    }

    /** 티켓 상태 변경 (WAITING, IN_PROGRESS, CLOSED) */
    @PatchMapping("/tickets/{ticketNo}/status")
    public TicketView status(@PathVariable String ticketNo, @RequestBody StatusRequest request) {
        return handoffService.changeStatus(ticketNo, request.status());
    }

    /** 상담원 답장 (한국어) → 고객 언어로 번역해 저장 */
    @PostMapping("/tickets/{ticketNo}/reply")
    public TicketView reply(@PathVariable String ticketNo, @RequestBody ReplyRequest request) {
        return handoffService.reply(ticketNo, request.text());
    }

    /** 상담 중 고객이 상담원에게 보내는 말 (고객 언어 그대로). 서버가 한국어로 번역해 저장한다 */
    @PostMapping("/tickets/{ticketNo}/customer-message")
    public MessageView customerMessage(@PathVariable String ticketNo, @RequestBody CustomerMessageRequest request) {
        return handoffService.customerMessage(ticketNo, request.text());
    }

    /** 고객이 상담 종료 버튼을 눌렀을 때: 티켓을 CLOSED로 바꾼다 (이미 종료됐어도 성공으로 응답) */
    @PostMapping("/tickets/{ticketNo}/customer-close")
    public TicketView customerClose(@PathVariable String ticketNo) {
        return handoffService.customerClose(ticketNo);
    }

    /** 고객 화면이 주기적으로 호출: 티켓 상태 + 새 상담원 답장(고객 언어) */
    @GetMapping("/tickets/{ticketNo}/agent-messages")
    public AgentPoll agentMessages(@PathVariable String ticketNo, @RequestParam(defaultValue = "0") long after) {
        return handoffService.poll(ticketNo, after);
    }
}
