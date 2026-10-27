package com.example.bankguide.suggest;

import com.example.bankguide.common.Masking;
import com.example.bankguide.domain.ChatMessage;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import com.example.bankguide.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 상담 중 다음에 보낼 만한 문장을 대화 맥락에서 만들어 추천한다.
 *  - role=agent    : 상담원이 보낼 답변 (한국어). 상담 요약·확인할 것·위험 표시도 참고한다
 *  - role=customer : 고객이 보낼 말 (고객 언어)
 * LLM이 실패하거나 쓸 만한 문장이 모자라면 빈 목록을 돌려준다. 화면은 그때 고정 예시를 보여준다.
 */
@Service
public class SuggestionService {

    /** LLM이 이 모양의 JSON으로 답한다 */
    public record Suggestions(List<String> suggestions) {}

    /** 화면에 돌려주는 모양. source는 LLM 또는 NONE(추천 없음) */
    public record SuggestionList(List<String> suggestions, String source) {}

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);
    private static final int MAX_MESSAGES = 12;
    private static final int MAX_SUGGESTIONS = 3;
    private static final int MAX_LENGTH = 120;
    private static final SuggestionList NONE = new SuggestionList(List.of(), "NONE");

    private static final String AGENT_PROMPT = """
            You help a Korean bank call-center agent who is chatting with a foreign customer.
            Suggest exactly 3 different short replies the agent could send next.

            - Write in polite Korean, one sentence each, concrete and easy to translate.
            - Base them only on the conversation and the handoff note. Answer what the customer last asked.
            - Do not promise results, amounts or dates that are not in the conversation.
            - Never ask for passwords, PINs, security card numbers, account numbers, resident or foreigner
              registration numbers (even the last digits), phone numbers or any other ID number.
              If identity must be checked, tell the customer to use the app's verification or visit a branch.
            - Ask whether someone else requested a transfer or account loan only when the note says FRAUD_SUSPECTED.
            - If the note says FRAUD_SUSPECTED, one of the replies must check whether someone else asked
              the customer to transfer money or lend their account.
            - If the conversation is ending, one reply may politely close the chat.
            """;

    private static final String CUSTOMER_PROMPT = """
            You help a foreign customer who is chatting with a Korean bank agent.
            Suggest exactly 3 different short messages the customer could send next.

            - Write in the language of the given language code, in simple everyday words, first person.
            - Base them only on the conversation. React to the agent's last message (answer a question,
              ask to explain again, confirm, or say thanks).
            - Never include account numbers, passwords or other personal data.
            """;

    private final ChatClient chatClient;
    private final HandoffTicketRepository ticketRepository;
    private final ConversationService conversationService;
    // 같은 대화 상태에서 화면이 여러 번 물어도 LLM은 한 번만 부른다
    private final Map<String, SuggestionList> cache = new ConcurrentHashMap<>();

    public SuggestionService(ChatClient.Builder builder,
                             HandoffTicketRepository ticketRepository,
                             ConversationService conversationService) {
        this.chatClient = builder.build();
        this.ticketRepository = ticketRepository;
        this.conversationService = conversationService;
    }

    public SuggestionList suggest(String ticketNo, String role) {
        boolean forAgent = "agent".equals(role);
        if (!forAgent && !"customer".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be agent or customer");
        }
        HandoffTicket t = ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketNo));
        if ("CLOSED".equals(t.getStatus())) {
            return NONE;
        }

        List<ChatMessage> history = conversationService.history(t.getConversationId()).stream()
                .filter(m -> !"SYSTEM".equals(m.getSender()))
                .toList();
        if (history.isEmpty()) {
            return NONE;
        }
        history = history.subList(Math.max(0, history.size() - MAX_MESSAGES), history.size());

        String key = ticketNo + "|" + role + "|" + history.get(history.size() - 1).getId();
        SuggestionList cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        SuggestionList result;
        try {
            String user = forAgent ? agentInput(t, history) : customerInput(t, history);
            Suggestions raw = chatClient.prompt()
                    .system(forAgent ? AGENT_PROMPT : CUSTOMER_PROMPT)
                    .user(user)
                    .call()
                    .entity(Suggestions.class);
            List<String> cleaned = clean(raw == null ? null : raw.suggestions());
            result = cleaned.size() >= 2 ? new SuggestionList(cleaned, "LLM") : NONE;
        } catch (Exception e) {
            log.warn("[추천 문장 LLM 실패 → 고정 예시 사용] {}", e.getMessage());
            result = NONE;
        }
        if (cache.size() > 500) {
            cache.clear();
        }
        cache.put(key, result);
        return result;
    }

    /** 상담원에게 줄 입력: 상담 요약 + 확인할 것 + 위험 표시 + 대화(고객 말은 한국어 번역이 있으면 그것) */
    static String agentInput(HandoffTicket t, List<ChatMessage> history) {
        return "Handoff note\n"
                + "summary: " + Masking.mask(t.getSummaryKo()).replace('\n', ' ') + "\n"
                + "checkpoints: " + Masking.mask(t.getCheckpoints()).replace('\n', ' ') + "\n"
                + "riskFlag: " + t.getRiskFlag() + "\n\n"
                + "Conversation (the agent is the one who will send the next reply)\n"
                + transcript(history, true);
    }

    /** 고객에게 줄 입력: 언어 코드 + 대화(상담원 말은 고객 언어 번역이 있으면 그것) */
    static String customerInput(HandoffTicket t, List<ChatMessage> history) {
        return "language code: " + t.getLang() + "\n\n"
                + "Conversation (the customer is the one who will send the next message)\n"
                + transcript(history, false);
    }

    static String transcript(List<ChatMessage> history, boolean forAgent) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : history) {
            String who;
            String text;
            switch (m.getSender()) {
                case "CUSTOMER" -> {
                    who = forAgent ? "Customer" : "Customer (me)";
                    text = forAgent && m.getTranslated() != null ? m.getTranslated() : m.getContent();
                }
                case "AGENT" -> {
                    who = forAgent ? "Agent (me)" : "Agent";
                    text = !forAgent && m.getTranslated() != null ? m.getTranslated() : m.getContent();
                }
                default -> {
                    who = "AI guide";
                    text = m.getContent();
                }
            }
            sb.append("- ").append(who).append(": ").append(Masking.mask(text)).append('\n');
        }
        return sb.toString();
    }

    /** 앞뒤 공백·따옴표·번호를 정리하고, 빈 것·중복·너무 긴 것을 빼서 최대 3개만 남긴다 */
    static List<String> clean(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim()
                    .replaceFirst("^\\s*(?:\\d+[.)]|[-•*])\\s*", "")
                    .replaceAll("^[\"“”'‘’]+|[\"“”'‘’]+$", "")
                    .trim();
            if (!t.isEmpty() && t.length() <= MAX_LENGTH) {
                out.add(t);
            }
            if (out.size() == MAX_SUGGESTIONS) {
                break;
            }
        }
        return List.copyOf(out);
    }
}
