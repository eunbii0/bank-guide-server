package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * eligibility_check 테이블: 자격 판단 기록.
 * 상담원 연결 쪽(팀원 담당)이 conversationId로 찾아 티켓에 붙일 재료다. LLM 요약 입력에는 넣지 않는다.
 */
@Entity
@Table(name = "eligibility_check")
public class EligibilityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conversationId;
    private String customerId;
    private String category;
    private boolean confirmed;
    private String handoffReason; // PROFILE_MISMATCH, NOT_ELIGIBLE, null
    private String resultKo;
    private String resultJson;
    private LocalDateTime createdAt;

    protected EligibilityCheck() {}

    public EligibilityCheck(String conversationId, String customerId, String category, boolean confirmed,
                            String handoffReason, String resultKo, String resultJson) {
        this.conversationId = conversationId;
        this.customerId = customerId;
        this.category = category;
        this.confirmed = confirmed;
        this.handoffReason = handoffReason;
        this.resultKo = resultKo;
        this.resultJson = resultJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getCustomerId() { return customerId; }
    public String getCategory() { return category; }
    public boolean isConfirmed() { return confirmed; }
    public String getHandoffReason() { return handoffReason; }
    public String getResultKo() { return resultKo; }
    public String getResultJson() { return resultJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
