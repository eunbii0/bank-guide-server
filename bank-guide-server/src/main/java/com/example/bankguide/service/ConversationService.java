package com.example.bankguide.service;

import com.example.bankguide.domain.ChatMessage;
import com.example.bankguide.domain.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** 채팅 기록 저장·조회. 고객이 쓴 말은 원문 그대로 남긴다 (요약 검증, 분쟁 확인용). */
@Service
public class ConversationService {

    private final ChatMessageRepository repository;

    public ConversationService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    public void customer(String conversationId, String lang, String text) {
        save(conversationId, "CUSTOMER", lang, text);
    }

    public void ai(String conversationId, String lang, String text) {
        save(conversationId, "AI", lang, text);
    }

    /** 시스템 안내(예: 고객이 상담을 종료함). 상담원 화면 대화에 한국어로 남긴다 */
    public void system(String conversationId, String text) {
        save(conversationId, "SYSTEM", "ko", text);
    }

    /** 상담 중 고객이 앱 화면을 옮긴 기록. content에는 화면 ID를 두고, 보여줄 때 화면 이름으로 바꾼다 */
    public ChatMessage activity(String conversationId, String page) {
        return repository.save(new ChatMessage(conversationId, "ACTIVITY", "ko", page));
    }

    /** 상담 중 고객이 상담원에게 쓴 말: 원문과 한국어 번역(없으면 null)을 함께 저장한다 */
    public ChatMessage customerToAgent(String conversationId, String lang, String original, String koreanText) {
        String content = original.length() > 1000 ? original.substring(0, 1000) : original;
        String trans = koreanText == null || koreanText.length() <= 1000 ? koreanText : koreanText.substring(0, 1000);
        return repository.save(new ChatMessage(conversationId, "CUSTOMER", lang, content, trans));
    }

    /** 상담원 답장: 원문(한국어)과 고객 언어 번역을 함께 저장한다 */
    public ChatMessage agent(String conversationId, String original, String translated) {
        String content = original.length() > 1000 ? original.substring(0, 1000) : original;
        String trans = translated == null || translated.length() <= 1000 ? translated : translated.substring(0, 1000);
        return repository.save(new ChatMessage(conversationId, "AGENT", "ko", content, trans));
    }

    /** afterId 이후에 쌓인 상담원 답장 (고객 화면이 주기적으로 가져간다) */
    public List<ChatMessage> agentMessagesAfter(String conversationId, long afterId) {
        return repository.findByConversationIdAndSenderAndIdGreaterThanOrderByIdAsc(conversationId, "AGENT", afterId);
    }

    public List<ChatMessage> history(String conversationId) {
        return conversationId == null ? List.of() : repository.findByConversationIdOrderByIdAsc(conversationId);
    }

    private void save(String conversationId, String sender, String lang, String text) {
        if (conversationId == null || text == null || text.isBlank()) {
            return;
        }
        String content = text.length() > 1000 ? text.substring(0, 1000) : text;
        repository.save(new ChatMessage(conversationId, sender, lang, content));
    }
}
