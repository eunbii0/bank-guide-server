package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** guide_task_text 테이블: 업무 이름과 확인 질문 (언어별). */
@Entity
@Table(name = "guide_task_text")
public class GuideTaskText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String taskCode;
    private String lang;
    private String name;
    private String confirmText;

    protected GuideTaskText() {}

    public Long getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public String getLang() { return lang; }
    public String getName() { return name; }
    public String getConfirmText() { return confirmText; }
}
