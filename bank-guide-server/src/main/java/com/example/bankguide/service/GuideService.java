package com.example.bankguide.service;

import com.example.bankguide.api.ApiDtos.EventResponse;
import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.StartResponse;
import com.example.bankguide.api.ApiDtos.StuckResponse;
import com.example.bankguide.domain.GuideEvent;
import com.example.bankguide.domain.GuideEventRepository;
import com.example.bankguide.domain.GuideStep;
import com.example.bankguide.domain.GuideStepRepository;
import com.example.bankguide.domain.GuideTask;
import com.example.bankguide.domain.GuideTaskRepository;
import com.example.bankguide.engine.Guide;
import com.example.bankguide.engine.GuideEngine;
import com.example.bankguide.engine.GuideEngine.Route;
import com.example.bankguide.engine.GuideEngine.Step;
import com.example.bankguide.engine.GuideSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 안내 세션 관리: 시작 → 화면 진입마다 다음 안내 → 종료.
 * 진행 상태(업무·언어·단계)는 메모리에, 행동 기록은 DB(guide_event)에 남긴다.
 */
@Service
public class GuideService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int OFF_PATH_LIMIT = 3;
    private static final String TERM = "{term}";

    private final GuideTaskRepository taskRepository;
    private final GuideStepRepository stepRepository;
    private final GuideEventRepository eventRepository;
    private final TextService textService;

    // 데모용 세션 저장소. 서버를 재시작하면 사라진다.
    private final Map<String, GuideSession> sessions = new ConcurrentHashMap<>();

    public GuideService(GuideTaskRepository taskRepository,
                        GuideStepRepository stepRepository,
                        GuideEventRepository eventRepository,
                        TextService textService) {
        this.taskRepository = taskRepository;
        this.stepRepository = stepRepository;
        this.eventRepository = eventRepository;
        this.textService = textService;
    }

    /**
     * term: 예금·적금 가입 기간(개월). 단계에 {term}이 있는 업무는 꼭 필요하다.
     * conversationId: 이 안내를 시작한 대화. 상담원 요약이 대화 안의 모든 안내 기록을 모을 때 쓴다
     */
    public StartResponse start(String conversationId, String intent, String page, String lang, Integer term) {
        String l = textService.normalize(lang);
        Route route = loadRoute(intent, l, term);
        GuideSession session = new GuideSession(UUID.randomUUID().toString(), conversationId, intent, l, term);
        sessions.put(session.getSessionId(), session);

        Guide guide = enter(session, route, page);
        String detail = intent + (term == null ? "" : " " + term + "개월") + " (" + l + ")";
        record(session, "GUIDE_START", page, detail);
        return new StartResponse(session.getSessionId(), guide);
    }

    public EventResponse onPageEnter(String sessionId, String page) {
        GuideSession session = findSession(sessionId);
        Guide guide = enter(session, loadRoute(session.getIntent(), session.getLang(), session.getTerm()), page);

        record(session, "PAGE_ENTER", page, describe(guide));
        if ("DONE".equals(guide.type())) {
            sessions.remove(sessionId);
        }

        // 4단계: 경로 이탈이 딱 3번째가 되는 순간 한 번만 상담원 연결을 제안한다
        String offer = null;
        if ("OFF_PATH".equals(guide.type()) && session.getOffPathCount() == OFF_PATH_LIMIT) {
            offer = textService.ui("OFFER_STUCK", session.getLang());
            record(session, "OFFER", page, "OFF_PATH x" + OFF_PATH_LIMIT);
        }
        return new EventResponse(guide, offer);
    }

    /** 4단계: 화면이 "같은 화면에서 N초 동안 아무것도 안 눌렀다"고 알려옴 → 기록하고 연결 제안 문구를 준다 */
    public StuckResponse stuck(String sessionId, String page, int idleSec) {
        GuideSession session = findSession(sessionId);
        record(session, "STUCK", page, "idle " + idleSec + "s");
        return new StuckResponse(textService.ui("OFFER_STUCK", session.getLang()));
    }

    public void stop(String sessionId) {
        GuideSession session = sessionId == null ? null : sessions.remove(sessionId);
        if (session != null) {
            record(session, "GUIDE_STOP", null, null);
        }
    }

    /** 4단계: 상담원에게 넘어가면 안내를 끝낸다 (하이라이트 중지) */
    public void endForHandoff(String sessionId, String reason) {
        GuideSession session = sessionId == null ? null : sessions.remove(sessionId);
        if (session != null) {
            record(session, "HANDOFF", null, reason);
        }
    }

    public Optional<GuideSession> session(String sessionId) {
        return sessionId == null ? Optional.empty() : Optional.ofNullable(sessions.get(sessionId));
    }

    public List<EventView> events(String sessionId) {
        return eventRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .map(e -> new EventView(e.getCreatedAt().format(TIME), e.getEventType(), e.getPage(), e.getDetail()))
                .toList();
    }

    private void record(GuideSession session, String type, String page, String detail) {
        eventRepository.save(new GuideEvent(session.getConversationId(), session.getSessionId(), type, page, detail));
    }

    private Guide enter(GuideSession session, Route route, String page) {
        GuideEngine.Result result = GuideEngine.onPageEnter(route, page);
        session.apply(result);
        return result.guide();
    }

    /**
     * DB에서 업무 경로와 고객 언어의 문구를 읽어 엔진이 쓰는 Route로 만든다.
     * 버튼 id·문구의 {term}은 고객이 고른 가입 기간으로 바꾼다. 예) btn-term-{term} → btn-term-6
     */
    private Route loadRoute(String intent, String lang, Integer term) {
        GuideTask task = taskRepository.findById(intent)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service: " + intent));
        Map<Integer, String> messages = textService.stepMessages(intent, lang);

        List<GuideStep> rows = stepRepository.findByTaskCodeOrderByStepNoAsc(intent);
        if (term == null && rows.stream().anyMatch(s -> s.getTarget().contains(TERM))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "term is required for " + intent);
        }
        String months = term == null ? "" : String.valueOf(term);
        List<Step> steps = rows.stream()
                .map(s -> new Step(s.getPage(), s.getTarget().replace(TERM, months),
                        messages.getOrDefault(s.getStepNo(), "").replace(TERM, months)))
                .toList();
        String name = textService.task(intent, lang).getName();

        return new Route(task.getGoalPage(), steps,
                textService.ui("OFF_PATH", lang),
                textService.ui("DONE", lang).replace("{name}", name));
    }

    private GuideSession findSession(String sessionId) {
        GuideSession session = sessionId == null ? null : sessions.get(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guide session not found. Please start again.");
        }
        return session;
    }

    private String describe(Guide guide) {
        return guide.step() == null ? guide.type() : guide.type() + " " + guide.step() + "/" + guide.total();
    }
}
