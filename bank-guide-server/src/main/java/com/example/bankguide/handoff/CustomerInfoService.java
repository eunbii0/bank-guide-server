package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.CustomerInfoView;
import com.example.bankguide.api.ApiDtos.EligibilityRecordView;
import com.example.bankguide.api.ApiDtos.InfoAccessView;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.domain.CustomerInfoAccess;
import com.example.bankguide.domain.CustomerInfoAccessRepository;
import com.example.bankguide.domain.CustomerProfile;
import com.example.bankguide.domain.CustomerProfileRepository;
import com.example.bankguide.domain.EligibilityCheckRepository;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import com.example.bankguide.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 고객 정보 공유 (상담 중 동의).
 * 1. 상담원이 상담 중에 [고객 정보 확인 요청]을 누른다 → REQUESTED
 * 2. 고객 화면에 [허용]/[거절] 카드가 뜬다 → GRANTED / DENIED
 * 3. 허용된 동안(상담이 끝나기 전)만 상담원이 고객 정보·자격 판단 결과를 볼 수 있고, 볼 때마다 조회 기록을 남긴다
 *
 * 고객 정보는 채팅 기록에 넣지 않는다. 채팅 기록은 LLM 요약·답변 추천의 재료라서, 넣으면 체류자격이 LLM으로 나간다.
 */
@Service
public class CustomerInfoService {

    public static final String NONE = "NONE";
    public static final String REQUESTED = "REQUESTED";
    public static final String GRANTED = "GRANTED";
    public static final String DENIED = "DENIED";

    private static final Logger log = LoggerFactory.getLogger(CustomerInfoService.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String ITEMS = "PROFILE,ELIGIBILITY";
    private static final String DEFAULT_AGENT = "agent-01"; // 로그인이 없어서 화면이 상담원 id를 보내지 않으면 이 값

    private final HandoffService handoffService;
    private final HandoffTicketRepository ticketRepository;
    private final CustomerProfileRepository customerRepository;
    private final EligibilityCheckRepository checkRepository;
    private final CustomerInfoAccessRepository accessRepository;
    private final ConversationService conversationService;

    public CustomerInfoService(HandoffService handoffService,
                               HandoffTicketRepository ticketRepository,
                               CustomerProfileRepository customerRepository,
                               EligibilityCheckRepository checkRepository,
                               CustomerInfoAccessRepository accessRepository,
                               ConversationService conversationService) {
        this.handoffService = handoffService;
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.checkRepository = checkRepository;
        this.accessRepository = accessRepository;
        this.conversationService = conversationService;
    }

    /** 상담원: 고객에게 정보 확인을 요청한다. 거절된 뒤 다시 요청할 수도 있다 */
    public TicketView request(String ticketNo) {
        HandoffTicket t = open(ticketNo);
        if (t.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "고객 정보가 연결되지 않은 티켓입니다: " + ticketNo);
        }
        if (!GRANTED.equals(t.getInfoConsent()) && !REQUESTED.equals(t.getInfoConsent())) {
            t.setInfoConsent(REQUESTED);
            ticketRepository.save(t);
            conversationService.system(t.getConversationId(), "상담원이 고객 정보 확인을 요청했습니다.");
            log.info("[정보 확인 요청] {}", ticketNo);
        }
        return handoffService.view(t);
    }

    /** 고객: 요청에 답한다. 요청이 없는데 답하면 409 */
    public TicketView answer(String ticketNo, boolean granted) {
        HandoffTicket t = open(ticketNo);
        if (!REQUESTED.equals(t.getInfoConsent())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No pending info request: " + ticketNo);
        }
        t.setInfoConsent(granted ? GRANTED : DENIED);
        ticketRepository.save(t);
        conversationService.system(t.getConversationId(),
                granted ? "고객이 고객 정보 확인을 허용했습니다." : "고객이 고객 정보 확인을 거절했습니다.");
        log.info("[정보 확인 {}] {}", granted ? "허용" : "거절", ticketNo);
        return handoffService.view(t);
    }

    /** 상담원: 허용된 상담에서만 고객 정보를 돌려주고, 조회 기록을 남긴다 */
    public CustomerInfoView info(String ticketNo, String agentId) {
        HandoffTicket t = handoffService.find(ticketNo);
        if (!GRANTED.equals(t.getInfoConsent()) || "CLOSED".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "고객이 정보 확인을 허용한 진행 중 상담에서만 볼 수 있습니다.");
        }
        CustomerProfile c = customerRepository.findById(t.getCustomerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        String agent = agentId == null || agentId.isBlank() ? DEFAULT_AGENT : agentId.trim();
        accessRepository.save(new CustomerInfoAccess(ticketNo, c.getCustomerId(), agent, ITEMS));
        log.info("[고객 정보 조회] ticket={} customer={} agent={}", ticketNo, c.getCustomerId(), agent);

        List<EligibilityRecordView> checks = checkRepository.findByConversationIdOrderByIdAsc(t.getConversationId()).stream()
                .map(e -> new EligibilityRecordView(e.getCreatedAt().format(TIME), e.getCategory(), e.isConfirmed(),
                        e.getResultKo() == null ? List.of() : Arrays.asList(e.getResultKo().split("\n"))))
                .toList();
        List<InfoAccessView> accessLog = accessRepository.findByTicketNoOrderByIdAsc(ticketNo).stream()
                .map(a -> new InfoAccessView(a.getAccessedAt().format(TIME), a.getAgentId(), a.getItems()))
                .toList();
        return new CustomerInfoView(c.getCustomerId(), c.getName(), c.getNationalityKo(), c.getVisaType(),
                c.getStayExpiry() == null ? null : c.getStayExpiry().toString(),
                c.isHasArc(), c.isHasAccount(), c.isIncomeVerified(), checks, accessLog);
    }

    private HandoffTicket open(String ticketNo) {
        HandoffTicket t = handoffService.find(ticketNo);
        if ("CLOSED".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket is already closed: " + ticketNo);
        }
        return t;
    }
}
