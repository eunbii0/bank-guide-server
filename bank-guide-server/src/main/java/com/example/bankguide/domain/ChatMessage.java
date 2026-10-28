package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** chat_message 테이블: 고객·AI·상담원이 주고받은 말 (원문 그대로). */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conversationId;
    private String sender;   // CUSTOMER, AI, AGENT
    private String lang;
    private String content;
    private String translated;   // 상담원 답장을 고객 언어로 번역한 문장 (그 외에는 null)
    private LocalDateTime createdAt;

    protected ChatMessage() {}

    public ChatMessage(String conversationId, String sender, String lang, String content) {
        this(conversationId, sender, lang, content, null);
    }

    public ChatMessage(String conversationId, String sender, String lang, String content, String translated) {
        this.conversationId = conversationId;
        this.sender = sender;
        this.lang = lang;
        this.content = content;
        this.translated = translated;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getSender() { return sender; }
    public String getLang() { return lang; }
    public String getContent() { return content; }
    public String getTranslated() { return translated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
