package com.example.bankguide.intent;

import com.example.bankguide.api.ApiDtos.ChatResponse;
import com.example.bankguide.api.ApiDtos.Option;
import com.example.bankguide.common.Masking;
import com.example.bankguide.domain.ChatMessage;
import com.example.bankguide.domain.CustomerProfile;
import com.example.bankguide.domain.GuideTask;
import com.example.bankguide.domain.GuideTaskRepository;
import com.example.bankguide.domain.GuideTaskText;
import com.example.bankguide.eligibility.EligibilityService;
import com.example.bankguide.service.ConversationService;
import com.example.bankguide.service.TextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 고객 문장 → 업무 분류 + 이관 판단.
 *
 * 1. 고객 원문을 채팅 기록에 저장한다
 * 2. 개인정보로 보이는 숫자를 가린 뒤 LLM에게 분류를 맡긴다 (실패하면 키워드 분류로 대신)
 * 3. LLM 답을 그대로 믿지 않고, DB에 있는 업무 코드인지 검사한다
 * 4. 금융사기 의심(FRAUD)·판단 질문(ADVICE)이면 업무 안내보다 상담원 연결을 먼저 한다
 * 5. 고객 언어로 응답을 만들고, 그 응답도 채팅 기록에 저장한다
 * 6. 카드 발급·상품 가입 업무면 확인 질문 대신 고객 정보 확인(VERIFY)으로 보낸다 → 가입 자격 판단
 */
@Service
public class IntentService {

    public static final String UNKNOWN = "UNKNOWN";
    public static final String AMBIGUOUS = "AMBIGUOUS";
    public static final String MENU = "MENU";
    public static final String VERIFY = "VERIFY";

    // 챗봇 첫 메뉴 버튼. 상품 해지는 자격 판단 없이 기존 정기예금 중도해지로 간다.
    private static final List<String> MENU_ITEMS = List.of("MENU_CARD", "MENU_CANCEL", "MENU_JOIN");
    // 위 세 가지 밖의 일: 업무로 이어지지 않고, 고객이 직접 쓰도록 안내만 한다
    private static final String MENU_OTHER = "MENU_OTHER";
    private static final String CANCEL_TASK = "DEPOSIT_CANCEL";
    private static final int MAX_CANDIDATES = 3;

    private static final Logger log = LoggerFactory.getLogger(IntentService.class);

    private final GuideTaskRepository taskRepository;
    private final LlmIntentClassifier llmClassifier;
    private final KeywordIntentClassifier keywordClassifier;
    private final TextService textService;
    private final ConversationService conversationService;
    private final EligibilityService eligibilityService;

    public IntentService(GuideTaskRepository taskRepository,
                         LlmIntentClassifier llmClassifier,
                         KeywordIntentClassifier keywordClassifier,
                         TextService textService,
                         ConversationService conversationService,
                         EligibilityService eligibilityService) {
        this.taskRepository = taskRepository;
        this.llmClassifier = llmClassifier;
        this.keywordClassifier = keywordClassifier;
        this.textService = textService;
        this.conversationService = conversationService;
        this.eligibilityService = eligibilityService;
    }

    /** 챗봇을 열었을 때: "도움이 필요하세요?" + 메뉴 버튼 4개(맨 아래는 기타 문의). 언어가 없으면 고객 선호 언어 */
    public ChatResponse menu(String conversationId, String customerId, String lang) {
        CustomerProfile customer = eligibilityService.customer(customerId);
        String l = textService.normalize(lang == null || lang.isBlank() ? customer.getPreferredLang() : lang);
        return reply(conversationId, menuResponse(l, "MENU_GREETING", "MENU"));
    }

    /**
     * 인사·감사·작별에 대한 답. 인사·감사는 메뉴 버튼을 함께 보여주고("더 도와드릴까요?"), 작별은 글만 보낸다.
     * talk는 GREETING, THANKS, GOODBYE 중 하나이고 그 밖의 값(NONE, 빈 값)이면 빈 값을 돌려준다.
     */
    private Optional<ChatResponse> smallTalkResponse(String lang, String talk, String source) {
        if (talk == null) {
            return Optional.empty();
        }
        return switch (talk) {
            case "GREETING" -> Optional.of(menuResponse(lang, "GREETING_REPLY", source));
            case "THANKS" -> Optional.of(menuResponse(lang, "THANKS_REPLY", source));
            case "GOODBYE" -> Optional.of(new ChatResponse(
                    UNKNOWN, null, lang, textService.ui("GOODBYE_REPLY", lang), List.of(), source, "NONE"));
            default -> Optional.empty();
        };
    }

    /** 고객 말 직전에 챗봇이 한 말. LLM이 "아니요 괜찮아요" 같은 말을 대화 흐름 속에서 읽게 한다. 없으면 null */
    private String lastBotMessage(String conversationId) {
        List<ChatMessage> history = conversationService.history(conversationId);
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            if ("AI".equals(m.getSender()) && !EligibilityService.VERIFY_PLACEHOLDER.equals(m.getContent())) {
                return m.getContent();
            }
        }
        return null;
    }

    /** 메시지(ui_text 키) + 메뉴 버튼 4개 */
    private ChatResponse menuResponse(String lang, String messageKey, String source) {
        List<Option> options = Stream.concat(MENU_ITEMS.stream(), Stream.of(MENU_OTHER))
                .map(code -> new Option(code, textService.ui(code, lang))).toList();
        return new ChatResponse(MENU, null, lang, textService.ui(messageKey, lang), options, source, "NONE");
    }

    public ChatResponse classify(String conversationId, String text) {
        return classify(conversationId, text, null, null);
    }

    /**
     * customerId: 데모용 가상 고객 (카드·가입 업무의 고객 정보 확인에 쓴다. 없으면 A).
     * preferredLang: 고객이 화면에서 직접 고른 언어. 있으면 입력한 언어와 상관없이 그 언어로 답한다.
     */
    public ChatResponse classify(String conversationId, String text, String customerId, String preferredLang) {
        if (text == null || text.isBlank()) {
            return reply(conversationId, unknown(textService.isSupported(preferredLang) ? preferredLang : "en", "NONE"));
        }
        // 인사·감사·작별만 보낸 경우는 LLM을 부르기 전에 바로 답한다 (빠르고, 키가 없어도 된다)
        Optional<SmallTalk.Match> smallTalk = SmallTalk.match(text);
        if (smallTalk.isPresent()) {
            SmallTalk.Match talk = smallTalk.get();
            String lang = textService.isSupported(preferredLang)
                    ? preferredLang.trim().toLowerCase()
                    : talk.lang();
            conversationService.customer(conversationId, lang, text);
            return reply(conversationId, smallTalkResponse(lang, talk.kind().name(), "SMALL_TALK").orElseThrow());
        }
        List<GuideTask> tasks = taskRepository.findAllByOrderByPriorityAsc();
        String safeText = Masking.mask(text);

        IntentResult result;
        String source;
        try {
            result = llmClassifier.classify(safeText, tasks, lastBotMessage(conversationId));
            if (result == null) {
                throw new IllegalStateException("LLM 응답이 비어 있음");
            }
            source = "LLM";
            log.info("[LLM 분류] '{}' → {}", safeText, result);
        } catch (Exception e) {
            log.warn("[LLM 실패 → 키워드 분류] {}", e.getMessage());
            result = keywordClassifier.classify(safeText, tasks);
            source = "KEYWORD";
        }

        String lang = textService.isSupported(preferredLang)
                ? preferredLang.trim().toLowerCase()
                : textService.normalize(result.language());
        conversationService.customer(conversationId, lang, text);

        // 4단계: 사람에게 넘겨야 하면 업무 안내보다 먼저 처리
        String escalation = result.escalation() == null ? "NONE" : result.escalation();
        if ("FRAUD".equals(escalation)) {
            return reply(conversationId, new ChatResponse(
                    "FRAUD", null, lang, textService.ui("FRAUD_WARNING", lang), List.of(), source, "FRAUD"));
        }
        if ("ADVICE".equals(escalation)) {
            return reply(conversationId, new ChatResponse(
                    "ADVICE", null, lang, textService.ui("OFFER_ADVICE", lang), List.of(), source, "ADVICE"));
        }

        Set<String> validCodes = tasks.stream().map(GuideTask::getCode).collect(Collectors.toSet());

        // LLM이 목록에 없는 코드를 지어내도 걸러낸다
        List<String> candidates = result.candidates() == null ? List.of()
                : result.candidates().stream().filter(validCodes::contains).distinct().toList();
        String intent = validCodes.contains(result.intent()) ? result.intent() : null;
        if (intent == null && candidates.size() == 1) {
            intent = candidates.get(0);
        }
        // 고를 업무가 없고 인사·감사·작별이면 "무슨 업무인지 모르겠어요" 대신 대화하듯 답한다
        if (intent == null && candidates.isEmpty()) {
            Optional<ChatResponse> talk = smallTalkResponse(lang, result.talk(), source);
            if (talk.isPresent()) {
                return reply(conversationId, talk.get());
            }
        }
        // 후보가 너무 많으면 고객 말을 이해하지 못한 것이다 ("안녕하세요", "새 계좌 만들래요" 등).
        // 관련 없는 업무 버튼 7개를 늘어놓지 말고 모른다고 답한다.
        if (candidates.size() > MAX_CANDIDATES) {
            return reply(conversationId, unknown(lang, source));
        }

        if (candidates.size() >= 2) {
            List<Option> options = candidates.stream()
                    .map(code -> new Option(code, textService.task(code, lang).getName()))
                    .toList();
            return reply(conversationId,
                    new ChatResponse(AMBIGUOUS, null, lang, textService.ui("CLARIFY", lang), options, source, "NONE"));
        }
        if (intent != null) {
            Optional<String> category = eligibilityService.categoryOfTask(intent);
            if (category.isPresent()) {
                return verify(conversationId, customerId, lang, category.get(), source);
            }
            GuideTaskText taskText = textService.task(intent, lang);
            return reply(conversationId,
                    new ChatResponse(intent, taskText.getConfirmText(), lang, null, List.of(), source, "NONE"));
        }
        return reply(conversationId, unknown(lang, source));
    }

    /**
     * 선택지·메뉴 버튼을 눌렀을 때.
     *  - MENU_CANCEL: 기존 정기예금 중도해지 확인 질문
     *  - MENU_CARD / MENU_JOIN, 카드·가입 업무: 고객 정보 확인 질문 (VERIFY)
     *  - 그 밖의 업무: 확인 질문
     */
    public ChatResponse choose(String conversationId, String intent, String lang, String customerId) {
        String l = textService.normalize(lang);
        if (MENU_OTHER.equals(intent)) {
            conversationService.customer(conversationId, l, "[메뉴] " + textService.ui(intent, "ko"));
            return reply(conversationId,
                    new ChatResponse(UNKNOWN, null, l, textService.ui("MENU_OTHER_ASK", l), List.of(), "CHOICE", "NONE"));
        }
        String task = intent;
        if (MENU_ITEMS.contains(intent)) {
            conversationService.customer(conversationId, l, "[메뉴] " + textService.ui(intent, "ko"));
            if (!"MENU_CANCEL".equals(intent)) {
                return verify(conversationId, customerId, l, intent.substring("MENU_".length()), "CHOICE");
            }
            task = CANCEL_TASK;
        } else {
            if (!taskRepository.existsById(intent)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service: " + intent);
            }
            conversationService.customer(conversationId, l, "[선택] " + textService.task(intent, l).getName());
            Optional<String> category = eligibilityService.categoryOfTask(intent);
            if (category.isPresent()) {
                return verify(conversationId, customerId, l, category.get(), "CHOICE");
            }
        }
        GuideTaskText taskText = textService.task(task, l);
        return reply(conversationId,
                new ChatResponse(task, taskText.getConfirmText(), l, null, List.of(), "CHOICE", "NONE"));
    }

    /**
     * 고객 정보 확인 질문. 문구에 체류자격·만료일이 들어 있으므로 채팅 기록에는 자리표시만 남긴다
     * (채팅 기록은 상담원 연결 때 LLM 요약의 재료가 된다).
     */
    private ChatResponse verify(String conversationId, String customerId, String lang, String category, String source) {
        CustomerProfile customer = eligibilityService.customer(customerId);
        conversationService.ai(conversationId, lang, EligibilityService.VERIFY_PLACEHOLDER);
        return new ChatResponse(VERIFY, null, lang, eligibilityService.verifyQuestion(customer, lang),
                List.of(), source, "NONE", category);
    }

    private ChatResponse unknown(String lang, String source) {
        return new ChatResponse(UNKNOWN, null, lang, textService.ui("UNKNOWN", lang), List.of(), source, "NONE");
    }

    /** AI가 고객에게 한 말도 기록해 둔다 (요약할 때 대화 흐름이 보이도록) */
    private ChatResponse reply(String conversationId, ChatResponse response) {
        String said = response.confirmText() != null ? response.confirmText() : response.message();
        conversationService.ai(conversationId, response.lang(), said);
        return response;
    }
}
