package com.example.bankguide.eligibility;

import com.example.bankguide.eligibility.EligibilityEngine.Customer;
import com.example.bankguide.eligibility.EligibilityEngine.Failure;
import com.example.bankguide.eligibility.EligibilityEngine.Result;
import com.example.bankguide.eligibility.EligibilityEngine.Rule;
import com.example.bankguide.eligibility.EligibilityEngine.Term;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 데모용 가상 규칙 판단. 규칙은 data.sql의 product_rule과 같게 두고, 오늘은 2026-10-27로 고정한다. */
class EligibilityEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private static final List<Rule> CHECK_CARD = List.of(
            new Rule("REQUIRES_ARC", null, "RULE_NO_ARC", null),
            new Rule("REQUIRES_ACCOUNT", null, "RULE_NO_ACCOUNT", null),
            new Rule("VALID_UNTIL_EXPIRY", null, "NOTE_CARD_VALID", null));
    private static final List<Rule> CREDIT_CARD = List.of(
            new Rule("REQUIRES_ARC", null, "RULE_NO_ARC", "CHECK_CARD"),
            new Rule("VISA_IN", "F-2,F-4,F-5,F-6,E-7", "RULE_VISA", "CHECK_CARD"),
            new Rule("MIN_STAY_MONTHS", "6", "RULE_STAY_SHORT", "CHECK_CARD"),
            new Rule("INCOME_VERIFIED", null, "RULE_INCOME", "CHECK_CARD"));
    private static final List<Rule> SAVINGS = List.of(
            new Rule("REQUIRES_ACCOUNT", null, "RULE_NO_ACCOUNT", null),
            new Rule("MATURITY_BEFORE_EXPIRY", "6,12,24", "RULE_NO_TERM", null));

    // 고객 A/B/C (data.sql의 customer_profile과 같음)
    private static final Customer NGUYEN = new Customer("E-9", LocalDate.of(2027, 5, 31), true, true, true);
    private static final Customer LI_WEI = new Customer("F-5", null, true, true, true);
    private static final Customer AZIZ = new Customer("D-2", LocalDate.of(2026, 12, 15), true, true, false);

    @Test
    void 고객A_체크카드는_가능하고_유효기간은_체류_만료일까지() {
        Result r = EligibilityEngine.judge("CHECK_CARD", CHECK_CARD, NGUYEN, TODAY);

        assertTrue(r.eligible());
        assertEquals("NOTE_CARD_VALID", r.noteKey());
        assertEquals(LocalDate.of(2027, 5, 31), r.validUntil());
    }

    @Test
    void 고객A_신용카드는_체류자격_때문에_불가하고_체크카드를_추천한다() {
        Result r = EligibilityEngine.judge("CREDIT_CARD", CREDIT_CARD, NGUYEN, TODAY);

        assertFalse(r.eligible());
        assertEquals(List.of("RULE_VISA"), reasonKeys(r)); // 체류 7개월·소득 확인은 통과
        assertEquals("CHECK_CARD", r.altProduct());
    }

    @Test
    void 고객A_적금은_6개월만_가능하다() {
        Result r = EligibilityEngine.judge("SAVINGS", SAVINGS, NGUYEN, TODAY);

        assertTrue(r.eligible());
        assertEquals(List.of(6), availableMonths(r));
        assertEquals(LocalDate.of(2027, 10, 27), r.terms().get(1).maturity()); // 12개월 만기 > 2027-05-31
    }

    @Test
    void 고객B_영주권자는_모든_상품이_가능하다() {
        assertTrue(EligibilityEngine.judge("CHECK_CARD", CHECK_CARD, LI_WEI, TODAY).eligible());
        assertTrue(EligibilityEngine.judge("CREDIT_CARD", CREDIT_CARD, LI_WEI, TODAY).eligible());

        Result savings = EligibilityEngine.judge("SAVINGS", SAVINGS, LI_WEI, TODAY);
        assertTrue(savings.eligible());
        assertEquals(List.of(6, 12, 24), availableMonths(savings));
    }

    @Test
    void 고객B_만료일이_없으면_카드_유효기간_안내를_붙이지_않는다() {
        Result r = EligibilityEngine.judge("CHECK_CARD", CHECK_CARD, LI_WEI, TODAY);

        assertNull(r.noteKey());
        assertNull(r.validUntil());
    }

    @Test
    void 고객C_은행_기록대로라면_신용카드는_이유_세_가지로_불가하다() {
        Result r = EligibilityEngine.judge("CREDIT_CARD", CREDIT_CARD, AZIZ, TODAY);

        assertFalse(r.eligible());
        assertEquals(List.of("RULE_VISA", "RULE_STAY_SHORT", "RULE_INCOME"), reasonKeys(r));
    }

    @Test
    void 고객C_은행_기록대로라면_적금은_고를_기간이_없어_불가하다() {
        Result r = EligibilityEngine.judge("SAVINGS", SAVINGS, AZIZ, TODAY);

        assertFalse(r.eligible());
        assertEquals(List.of("RULE_NO_TERM"), reasonKeys(r));
        assertEquals(List.of(), availableMonths(r));
    }

    @Test
    void 남은_체류기간이_정확히_6개월이면_신용카드_가능() {
        Customer f2 = new Customer("F-2", TODAY.plusMonths(6), true, true, true);
        Customer oneDayShort = new Customer("F-2", TODAY.plusMonths(6).minusDays(1), true, true, true);

        assertTrue(EligibilityEngine.judge("CREDIT_CARD", CREDIT_CARD, f2, TODAY).eligible());
        assertFalse(EligibilityEngine.judge("CREDIT_CARD", CREDIT_CARD, oneDayShort, TODAY).eligible());
    }

    @Test
    void 만기일이_체류_만료일과_같은_날이면_그_기간은_선택_가능() {
        Customer c = new Customer("E-9", TODAY.plusMonths(12), true, true, true);

        assertEquals(List.of(6, 12), availableMonths(EligibilityEngine.judge("SAVINGS", SAVINGS, c, TODAY)));
    }

    @Test
    void 대안_상품이_없는_규칙만_실패하면_추천하지_않는다() {
        Customer noAccount = new Customer("F-5", null, true, false, true);

        Result r = EligibilityEngine.judge("SAVINGS", SAVINGS, noAccount, TODAY);
        assertFalse(r.eligible());
        assertNull(r.altProduct());
    }

    private static List<String> reasonKeys(Result r) {
        return r.failures().stream().map(Failure::reasonKey).toList();
    }

    private static List<Integer> availableMonths(Result r) {
        return r.terms().stream().filter(Term::available).map(Term::months).toList();
    }
}
