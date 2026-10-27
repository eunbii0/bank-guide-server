package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** ui_text 테이블: 업무와 무관한 시스템 문구 (OFF_PATH, DONE, UNKNOWN, CLARIFY). */
@Entity
@Table(name = "ui_text")
public class UiText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String textKey;
    private String lang;
    private String content;

    protected UiText() {}

    public Long getId() { return id; }
    public String getTextKey() { return textKey; }
    public String getLang() { return lang; }
    public String getContent() { return content; }
}
