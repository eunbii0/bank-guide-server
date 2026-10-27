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
