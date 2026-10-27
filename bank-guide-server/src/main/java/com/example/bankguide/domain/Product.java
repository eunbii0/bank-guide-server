package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** product 테이블: 가입 자격을 판단하는 상품. 상품 하나 = 화면 안내 업무(task_code) 하나. */
@Entity
@Table(name = "product")
public class Product {

    @Id
    private String code;
    private String category; // CARD, JOIN
    private String taskCode;
    private int sort;

    protected Product() {}

    public String getCode() { return code; }
    public String getCategory() { return category; }
    public String getTaskCode() { return taskCode; }
    public int getSort() { return sort; }
}
