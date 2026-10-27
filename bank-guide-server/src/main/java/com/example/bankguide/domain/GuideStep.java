package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** guide_step 테이블: 업무의 한 단계 (어느 화면에서 어떤 버튼을 누르는지). 안내 문구는 guide_step_text에 있다. */
@Entity
@Table(name = "guide_step")
public class GuideStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String taskCode;
    private int stepNo;
    private String page;
    private String target;

    protected GuideStep() {}

    public Long getId() { return id; }
    public String getTaskCode() { return taskCode; }
    public int getStepNo() { return stepNo; }
    public String getPage() { return page; }
    public String getTarget() { return target; }
}
