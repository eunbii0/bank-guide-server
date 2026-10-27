package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** guide_step_text 테이블: 단계별 안내 문구 (언어별). */
@Entity
@Table(name = "guide_step_text")
public class GuideStepText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String taskCode;
    private int stepNo;
    private String lang;
    private String message;

    protected GuideStepText() {}

    public Long getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public int getStepNo() { return stepNo; }
    public String getLang() { return lang; }
    public String getMessage() { return message; }
}
