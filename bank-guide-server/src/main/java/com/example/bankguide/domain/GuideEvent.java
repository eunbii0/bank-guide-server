package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** guide_event 테이블: 고객이 언제 어느 화면에 들어갔는지 기록. 4단계 요약의 재료가 된다. */
@Entity
@Table(name = "guide_event")
public class GuideEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sessionId;
    private String eventType;
    private String page;
    private String detail;
    private LocalDateTime createdAt;

    protected GuideEvent() {}

    public GuideEvent(String sessionId, String eventType, String page, String detail) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.page = page;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getEventType() { return eventType; }
    public String getPage() { return page; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
