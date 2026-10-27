package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** intent_keyword 테이블: 2단계용 의도 분류 키워드. */
@Entity
@Table(name = "intent_keyword")
public class IntentKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String taskCode;
    private String keyword;

    protected IntentKeyword() {}

    public Long getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public String getKeyword() { return keyword; }
}
