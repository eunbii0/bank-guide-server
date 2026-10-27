package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.AgentMessageView;
import com.example.bankguide.api.ApiDtos.AgentPoll;
import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.HandoffRequest;
import com.example.bankguide.api.ApiDtos.HandoffResponse;
import com.example.bankguide.api.ApiDtos.MessageView;
import com.example.bankguide.api.ApiDtos.TicketView;
import com.example.bankguide.common.PageNames;
import com.example.bankguide.domain.ChatMessage;
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
    private static final List<String> STATUSES = List.of("WAITING", "IN_PROGRESS", "CLOSED");

    private final GuideService guideService;
    private final ConversationService conversationService;
    private final TextService textService;
    private final GuideTaskRepository taskRepository;
    private final HandoffTicketRepository ticketRepository;
    private final HandoffSummarizer summarizer;
    private final AgentTranslator translator;

    public HandoffService(GuideService guideService,
                          ConversationService conversationService,
                          TextService textService,
                          GuideTaskRepository taskRepository,
                          HandoffTicketRepository ticketRepository,
                          HandoffSummarizer summarizer,
                          AgentTranslator translator) {
        this.translator = translator;
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

    /** 상담원 화면: 티켓 상태 변경 (WAITING → IN_PROGRESS → CLOSED) */
    public TicketView changeStatus(String ticketNo, String status) {
        if (!STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of " + STATUSES);
        }
        HandoffTicket t = find(ticketNo);
        t.setStatus(status);
        ticketRepository.save(t);
        return view(t);
    }

    /**
     * 상담원 답장: 한국어 원문을 저장하고 고객 언어로 번역해 함께 저장한다.
     * 번역이 실패하면 한국어 그대로 보낸다 (고객이 답을 못 받는 것보다 낫다).
     * 처음 답장하면 대기 중 티켓을 상담 중으로 바꾼다.
     */
    public TicketView reply(String ticketNo, String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        HandoffTicket t = find(ticketNo);
        if ("CLOSED".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket is already closed: " + ticketNo);
        }
        String original = text.trim();
        String translated;
        if ("ko".equals(t.getLang())) {
            translated = original;
        } else {
            try {
                translated = translator.translate(original, t.getLang());
                if (translated == null || translated.isBlank()) {
                    throw new IllegalStateException("번역 결과가 비어 있음");
                }
            } catch (Exception e) {
                log.warn("[번역 LLM 실패 → 한국어 원문 전달] {}", e.getMessage());
                translated = original;
            }
        }
        conversationService.agent(t.getConversationId(), original, translated);
        if ("WAITING".equals(t.getStatus())) {
            t.setStatus("IN_PROGRESS");
            ticketRepository.save(t);
        }
        return view(t);
    }

    /**
     * 고객이 직접 상담을 끝낸다: 티켓을 CLOSED로 바꾸고 상담원 화면 대화에 안내를 남긴다.
     * 이미 종료된 티켓이면 아무것도 하지 않는다 (상담원이 먼저 끝낸 경우에도 고객 화면이 오류 없이 마무리되도록).
     */
    public TicketView customerClose(String ticketNo) {
        HandoffTicket t = find(ticketNo);
        if (!"CLOSED".equals(t.getStatus())) {
            t.setStatus("CLOSED");
            ticketRepository.save(t);
            conversationService.system(t.getConversationId(), "고객이 상담을 종료했습니다.");
            log.info("[상담 종료] {} (고객이 종료)", ticketNo);
        }
        return view(t);
    }

    /**
     * 상담 중 고객이 상담원에게 보낸 말: 원문을 저장하고 한국어로 번역해 함께 저장한다.
     * 번역이 실패하면 번역 없이 원문만 저장한다 (상담원 화면에 원문이 그대로 보인다).
     */
    public MessageView customerMessage(String ticketNo, String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        HandoffTicket t = find(ticketNo);
        if ("CLOSED".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket is already closed: " + ticketNo);
        }
        String original = text.trim();
        String korean = null;
        if (!"ko".equals(t.getLang())) {
            try {
                korean = translator.toKorean(original, t.getLang());
                if (korean == null || korean.isBlank()) {
                    korean = null;
                }
            } catch (Exception e) {
                log.warn("[번역 LLM 실패 → 원문만 전달] {}", e.getMessage());
            }
        }
        ChatMessage saved = conversationService.customerToAgent(t.getConversationId(), t.getLang(), original, korean);
        return new MessageView(saved.getCreatedAt().format(TIME), saved.getSender(), saved.getLang(),
                saved.getContent(), saved.getTranslated());
    }

    /** 고객 화면: 티켓 상태 + afterId 이후의 상담원 답장(번역본) */
    public AgentPoll poll(String ticketNo, long afterId) {
        HandoffTicket t = find(ticketNo);
        List<AgentMessageView> messages = conversationService.agentMessagesAfter(t.getConversationId(), afterId).stream()
                .map(m -> new AgentMessageView(m.getId(), m.getCreatedAt().format(TIME),
                        m.getTranslated() != null ? m.getTranslated() : m.getContent()))
                .toList();
        return new AgentPoll(t.getStatus(), messages);
    }

    private HandoffTicket find(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
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
                .map(m -> new MessageView(m.getCreatedAt().format(TIME), m.getSender(), m.getLang(), m.getContent(),
                        "CUSTOMER".equals(m.getSender()) ? m.getTranslated() : null))
                .toList();
    }

    private static List<String> lines(String text) {
        return text == null || text.isBlank() ? List.of() : Arrays.asList(text.split("\n"));
    }
}
