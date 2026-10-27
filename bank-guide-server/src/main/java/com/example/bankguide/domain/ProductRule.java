package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** product_rule 테이블: 데모용 가상 규칙 한 줄. 규칙 종류는 EligibilityEngine 주석 참고. */
@Entity
@Table(name = "product_rule")
public class ProductRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productCode;
    private String ruleType;
    private String param;
    private String reasonKey;
    private String altProduct;

    protected ProductRule() {}

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public String getRuleType() { return ruleType; }
    public String getParam() { return param; }
    public String getReasonKey() { return reasonKey; }
    public String getAltProduct() { return altProduct; }
}
