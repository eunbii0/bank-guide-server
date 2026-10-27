package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * customer_profile 테이블: 데모용 가상 고객 (A, B, C).
 * 체류자격·만료일은 개인정보라서 LLM에 보내지 않는다. 자격 판단과 상담원 티켓에만 쓴다.
 */
@Entity
@Table(name = "customer_profile")
public class CustomerProfile {

    @Id
    private String customerId;
    private String name;
    private String nationalityKo;
    private String visaType;
    private LocalDate stayExpiry; // null = 만료 없음
    private boolean hasArc;
    private boolean hasAccount;
    private boolean incomeVerified;
    private String preferredLang;

    protected CustomerProfile() {}

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getNationalityKo() { return nationalityKo; }
    public String getVisaType() { return visaType; }
    public LocalDate getStayExpiry() { return stayExpiry; }
    public boolean isHasArc() { return hasArc; }
    public boolean isHasAccount() { return hasAccount; }
    public boolean isIncomeVerified() { return incomeVerified; }
    public String getPreferredLang() { return preferredLang; }
}
