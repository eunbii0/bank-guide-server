package com.example.bankguide.eligibility;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 가입 자격 판단 엔진. ※ 데모용 가상 규칙 (실제 은행 기준이 아님)
 *
 * GuideEngine처럼 Spring·DB를 모르는 순수 로직이다. 규칙은 product_rule 테이블에서 읽어 들어오고,
 * 오늘 날짜도 밖에서 받으므로 단위 테스트에서 날짜를 고정할 수 있다.
 * 판단은 이 규칙으로만 한다. LLM에게 맡기지 않는다.
 *
 * 규칙 종류 (rule_type)
 *  - REQUIRES_ARC            외국인등록증이 있어야 함
 *  - REQUIRES_ACCOUNT        우리 은행 계좌가 있어야 함
 *  - VISA_IN                 체류자격이 param 목록 중 하나 ("F-2,F-4,...")
 *  - MIN_STAY_MONTHS         오늘 + param개월 ≤ 체류 만료일 (정확히 param개월 남아도 통과)
 *  - INCOME_VERIFIED         소득 확인됨
 *  - MATURITY_BEFORE_EXPIRY  가입 기간 param("6,12,24") 중 만기일 ≤ 체류 만료일인 기간만 선택 가능. 하나도 없으면 불가
 *  - VALID_UNTIL_EXPIRY      불가 조건이 아님. 카드 유효기간 = 체류 만료일이라는 안내만 붙인다
 * 체류 만료일이 없는 고객(영주 등)은 기간 관련 규칙을 모두 통과한다.
 */
public final class EligibilityEngine {

    /** 판단에 쓰는 고객 정보 (이름·국적처럼 판단과 무관한 값은 받지 않는다) */
    public record Customer(String visaType, LocalDate stayExpiry,
                           boolean hasArc, boolean hasAccount, boolean incomeVerified) {}

    /** product_rule 한 줄 */
    public record Rule(String type, String param, String reasonKey, String altProduct) {}

    /** 불가 이유 한 건. reasonKey는 ui_text 키, 문구 채우기는 서비스가 한다 */
    public record Failure(String reasonKey, Rule rule) {}

    /** 가입 기간 하나. maturity = 오늘 + months */
    public record Term(int months, boolean available, LocalDate maturity) {}

    /**
     * 상품 하나의 판단 결과.
     * noteKey/validUntil: 가능하지만 알려줄 점 (카드 유효기간), altProduct: 불가일 때 추천 상품 코드
     */
    public record Result(String productCode, boolean eligible, List<Failure> failures,
                         List<Term> terms, String noteKey, LocalDate validUntil, String altProduct) {}

    private EligibilityEngine() {}

    public static Result judge(String productCode, List<Rule> rules, Customer customer, LocalDate today) {
        List<Failure> failures = new ArrayList<>();
        List<Term> terms = List.of();
        String noteKey = null;
        LocalDate validUntil = null;
        LocalDate expiry = customer.stayExpiry();

        for (Rule rule : rules) {
            switch (rule.type()) {
                case "REQUIRES_ARC" -> {
                    if (!customer.hasArc()) failures.add(new Failure(rule.reasonKey(), rule));
                }
                case "REQUIRES_ACCOUNT" -> {
                    if (!customer.hasAccount()) failures.add(new Failure(rule.reasonKey(), rule));
                }
                case "VISA_IN" -> {
                    if (!splitParam(rule.param()).contains(customer.visaType())) {
                        failures.add(new Failure(rule.reasonKey(), rule));
                    }
                }
                case "MIN_STAY_MONTHS" -> {
                    int months = Integer.parseInt(rule.param().trim());
                    if (!endsBy(today.plusMonths(months), expiry)) failures.add(new Failure(rule.reasonKey(), rule));
                }
                case "INCOME_VERIFIED" -> {
                    if (!customer.incomeVerified()) failures.add(new Failure(rule.reasonKey(), rule));
                }
                case "MATURITY_BEFORE_EXPIRY" -> {
                    terms = splitParam(rule.param()).stream()
                            .map(p -> Integer.parseInt(p.trim()))
                            .map(m -> new Term(m, endsBy(today.plusMonths(m), expiry), today.plusMonths(m)))
                            .toList();
                    if (terms.stream().noneMatch(Term::available)) failures.add(new Failure(rule.reasonKey(), rule));
                }
                case "VALID_UNTIL_EXPIRY" -> {
                    if (expiry != null) {
                        noteKey = rule.reasonKey();
                        validUntil = expiry;
                    }
                }
                default -> throw new IllegalArgumentException("모르는 규칙 종류: " + rule.type());
            }
        }

        boolean eligible = failures.isEmpty();
        String alt = eligible ? null : failures.stream()
                .map(f -> f.rule().altProduct())
                .filter(a -> a != null && !a.isBlank())
                .findFirst()
                .orElse(null);
        return new Result(productCode, eligible, List.copyOf(failures), terms,
                eligible ? noteKey : null, eligible ? validUntil : null, alt);
    }

    /** 만료일이 없으면 언제든 통과. 만기일이 만료일과 같은 날이면 통과 */
    private static boolean endsBy(LocalDate date, LocalDate expiry) {
        return expiry == null || !date.isAfter(expiry);
    }

    private static List<String> splitParam(String param) {
        return param == null ? List.of() : Arrays.stream(param.split(",")).map(String::trim).toList();
    }
}
