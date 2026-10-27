package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.HandoffRequest;
import com.example.bankguide.api.ApiDtos.HandoffResponse;
import com.example.bankguide.api.ApiDtos.MessageView;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.common.PageNames;
import com.example.bankguide.domain.GuideTaskRepository;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import com.example.bankguide.engine.GuideSession;
import com.example.bankguide.service.ConversationService;
import com.example.bankguide.service.GuideService;
import com.example.bankguide.service.TextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 상담원 연결 (4단계).
 * 1. 진행 중이던 안내를 끝낸다
 * 2. 채팅 원문 + 화면 이동 기록을 모은다
 * 3. LLM으로 한국어 요약을 만든다 (실패하면 규칙 요약)
 * 4. 언어·사유에 맞는 대기열로 티켓을 저장한다
 */
@Service
public class HandoffService {

    private static final Logger log = LoggerFactory.getLogger(HandoffService.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GuideService guideService;
    private final ConversationService conversationService;
    private final TextService textService;
    private final GuideTaskRepository taskRepository;
    private final HandoffTicketRepository ticketRepository;
    private final HandoffSummarizer summarizer;

    public HandoffService(GuideService guideService,
                          ConversationService conversationService,
                          TextService textService,
                          GuideTaskRepository taskRepository,
                          HandoffTicketRepository ticketRepository,
                          HandoffSummarizer summarizer) {
        this.guideService = guideService;
        this.conversationService = conversationService;
        this.textService = textService;
        this.taskRepository = taskRepository;
        this.ticketRepository = ticketRepository;
        this.summarizer = summarizer;
    }

    public HandoffResponse handoff(HandoffRequest req) {
        if (req.conversationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId is required");
        }
        String reason = req.reason() == null ? "CUSTOMER_REQUEST" : req.reason();

        // 언어·업무는 진행 중인 안내 세션이 있으면 그걸 우선한다
        Optional<GuideSession> session = guideService.session(req.sessionId());
        String lang = textService.normalize(session.map(GuideSession::getLang).orElse(req.lang()));
        String intent = session.map(GuideSession::getIntent).orElse(req.intent());
        String intentNameKo = intent != null && taskRepository.existsById(intent)
                ? textService.task(intent, "ko").getName() : null;

        guideService.endForHandoff(req.sessionId(), reason);

        List<EventView> events = req.sessionId() == null ? List.of() : guideService.events(req.sessionId());
        List<MessageView> messages = messages(req.conversationId());
        HandoffNote.Input input = new HandoffNote.Input(lang, intentNameKo, reason, req.page(), events, messages);

        SummaryResult summary;
        String source;
        try {
            summary = HandoffNote.sanitize(summarizer.summarize(input), reason);
            if (summary == null) {
                throw new IllegalStateException("LLM 요약이 비어 있음");
            }
            source = "LLM";
        } catch (Exception e) {
            log.warn("[요약 LLM 실패 → 규칙 요약] {}", e.getMessage());
            summary = HandoffNote.fallback(input);
            source = "RULE";
        }

        String queue = "FRAUD".equals(reason) || "FRAUD_SUSPECTED".equals(summary.riskFlag()) ? "FRAUD"
                : "ko".equals(lang) ? "GENERAL"
                : "FOREIGN";
        String ticketNo = "T-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", ticketRepository.count() + 1);

        ticketRepository.save(new HandoffTicket(ticketNo, req.conversationId(), req.sessionId(), queue, reason,
                lang, intent, req.page(), String.join("\n", summary.summary()),
                String.join("\n", summary.checkpoints()), summary.riskFlag(), source));
        log.info("[상담원 연결] {} queue={} reason={} lang={} summary={}", ticketNo, queue, reason, lang, source);

        String message = textService.ui("HANDOFF_DONE", lang);
        conversationService.ai(req.conversationId(), lang, message);
        return new HandoffResponse(ticketNo, queue, message);
    }

    public List<TicketView> tickets() {
        return ticketRepository.findAllByOrderByIdDesc().stream().map(this::view).toList();
    }

    public TicketView ticket(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketNo));
    }

    private TicketView view(HandoffTicket t) {
        String intentName = t.getIntent() != null && taskRepository.existsById(t.getIntent())
                ? textService.task(t.getIntent(), "ko").getName() : "미확인 업무";
        return new TicketView(t.getTicketNo(), t.getQueue(), t.getStatus(), HandoffNote.reasonKo(t.getReason()),
                HandoffNote.langKo(t.getLang()), intentName, PageNames.of(t.getCurrentPage()),
                lines(t.getSummaryKo()), lines(t.getCheckpoints()), t.getRiskFlag(), t.getSummarySource(),
                messages(t.getConversationId()), t.getCreatedAt().format(TIME));
    }

    private List<MessageView> messages(String conversationId) {
        return conversationService.history(conversationId).stream()
                .map(m -> new MessageView(m.getCreatedAt().format(TIME), m.getSender(), m.getLang(), m.getContent()))
                .toList();
    }

    private static List<String> lines(String text) {
        return text == null || text.isBlank() ? List.of() : Arrays.asList(text.split("\n"));
    }
}
