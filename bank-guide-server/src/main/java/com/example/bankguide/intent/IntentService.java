package com.example.bankguide.intent;

import com.example.bankguide.api.ApiDtos.ChatResponse;
import com.example.bankguide.api.ApiDtos.Option;
import com.example.bankguide.common.Masking;
import com.example.bankguide.domain.GuideTask;
import com.example.bankguide.domain.GuideTaskRepository;
import com.example.bankguide.domain.GuideTaskText;
import com.example.bankguide.service.ConversationService;
import com.example.bankguide.service.TextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고객 문장 → 업무 분류 + 이관 판단.
 *
 * 1. 고객 원문을 채팅 기록에 저장한다
 * 2. 개인정보로 보이는 숫자를 가린 뒤 LLM에게 분류를 맡긴다 (실패하면 키워드 분류로 대신)
 * 3. LLM 답을 그대로 믿지 않고, DB에 있는 업무 코드인지 검사한다
 * 4. 금융사기 의심(FRAUD)·판단 질문(ADVICE)이면 업무 안내보다 상담원 연결을 먼저 한다
 * 5. 고객 언어로 응답을 만들고, 그 응답도 채팅 기록에 저장한다
 */
@Service
public class IntentService {

    public static final String UNKNOWN = "UNKNOWN";
    public static final String AMBIGUOUS = "AMBIGUOUS";

    private static final Logger log = LoggerFactory.getLogger(IntentService.class);

    private final GuideTaskRepository taskRepository;
    private final LlmIntentClassifier llmClassifier;
    private final KeywordIntentClassifier keywordClassifier;
    private final TextService textService;
    private final ConversationService conversationService;

    public IntentService(GuideTaskRepository taskRepository,
                         LlmIntentClassifier llmClassifier,
                         KeywordIntentClassifier keywordClassifier,
                         TextService textService,
                         ConversationService conversationService) {
        this.taskRepository = taskRepository;
        this.llmClassifier = llmClassifier;
        this.keywordClassifier = keywordClassifier;
        this.textService = textService;
        this.conversationService = conversationService;
    }

    public ChatResponse classify(String conversationId, String text) {
        if (text == null || text.isBlank()) {
            return reply(conversationId, unknown("en", "NONE"));
        }
        List<GuideTask> tasks = taskRepository.findAllByOrderByPriorityAsc();
        String safeText = Masking.mask(text);

        IntentResult result;
        String source;
        try {
            result = llmClassifier.classify(safeText, tasks);
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

        String lang = textService.normalize(result.language());
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

        if (candidates.size() >= 2) {
            List<Option> options = candidates.stream()
                    .map(code -> new Option(code, textService.task(code, lang).getName()))
                    .toList();
            return reply(conversationId,
                    new ChatResponse(AMBIGUOUS, null, lang, textService.ui("CLARIFY", lang), options, source, "NONE"));
        }
        if (intent != null) {
            GuideTaskText taskText = textService.task(intent, lang);
            return reply(conversationId,
                    new ChatResponse(intent, taskText.getConfirmText(), lang, null, List.of(), source, "NONE"));
        }
        return reply(conversationId, unknown(lang, source));
    }

    /** 선택지 버튼을 눌렀을 때: 이미 고른 업무의 확인 질문만 만든다. */
    public ChatResponse choose(String conversationId, String intent, String lang) {
        String l = textService.normalize(lang);
        GuideTaskText taskText = textService.task(intent, l);
        conversationService.customer(conversationId, l, "[선택] " + taskText.getName());
        return reply(conversationId,
                new ChatResponse(intent, taskText.getConfirmText(), l, null, List.of(), "CHOICE", "NONE"));
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
