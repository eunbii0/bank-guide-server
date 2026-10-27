package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** handoff_ticket 테이블: 상담원에게 넘기는 한 건. 요약은 한국어, 원문은 chat_message에 있다. */
@Entity
@Table(name = "handoff_ticket")
public class HandoffTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticketNo;
    private String conversationId;
    private String sessionId;
    private String queue;
    private String reason;
    private String lang;
    private String intent;
    private String currentPage;
    private String summaryKo;
    private String checkpoints;
    private String riskFlag;
    private String summarySource;
    private String status;
    private String customerId;
    private String infoConsent; // NONE, REQUESTED, GRANTED, DENIED
    private LocalDateTime createdAt;

    protected HandoffTicket() {}

    public HandoffTicket(String ticketNo, String conversationId, String sessionId, String queue, String reason,
                         String lang, String intent, String currentPage, String summaryKo, String checkpoints,
                         String riskFlag, String summarySource, String customerId) {
        this.ticketNo = ticketNo;
        this.conversationId = conversationId;
        this.sessionId = sessionId;
        this.queue = queue;
        this.reason = reason;
        this.lang = lang;
        this.intent = intent;
        this.currentPage = currentPage;
        this.summaryKo = summaryKo;
        this.checkpoints = checkpoints;
        this.riskFlag = riskFlag;
        this.summarySource = summarySource;
        this.status = "WAITING";
        this.customerId = customerId;
        this.infoConsent = "NONE";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTicketNo() { return ticketNo; }
    public String getConversationId() { return conversationId; }
    public String getSessionId() { return sessionId; }
    public String getQueue() { return queue; }
    public String getReason() { return reason; }
    public String getLang() { return lang; }
    public String getIntent() { return intent; }
    public String getCurrentPage() { return currentPage; }
    public String getSummaryKo() { return summaryKo; }
    public String getCheckpoints() { return checkpoints; }
    public String getRiskFlag() { return riskFlag; }
    public String getSummarySource() { return summarySource; }
    public String getStatus() { return status; }
    public String getCustomerId() { return customerId; }
    public String getInfoConsent() { return infoConsent; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setInfoConsent(String infoConsent) { this.infoConsent = infoConsent; }
}
