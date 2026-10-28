package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** guide_task 테이블: 업무 하나 (예: DEPOSIT_CANCEL). 이름·문구는 언어별로 guide_task_text에 있다. */
@Entity
@Table(name = "guide_task")
public class GuideTask {

    @Id
    private String code;
    private String goalPage;
    private int priority;
    private String description; // LLM에게 보여줄 업무 설명

    protected GuideTask() {}

    public String getCode() { return code; }
    public String getGoalPage() { return goalPage; }
    public int getPriority() { return priority; }
    public String getDescription() { return description; }
}
