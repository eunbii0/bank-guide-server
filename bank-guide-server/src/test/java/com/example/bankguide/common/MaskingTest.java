package com.example.bankguide.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingTest {

    @Test
    void 계좌번호처럼_긴_숫자는_LLM에_보내기_전에_가린다() {
        assertEquals("Send money to *** please", Masking.mask("Send money to 110-234-567890 please"));
        assertEquals("전화번호 *** 로 연락", Masking.mask("전화번호 010 1234 5678 로 연락"));
    }

    @Test
    void 짧은_숫자와_금액은_그대로_둔다() {
        assertEquals("Chuyển 50,000 won vào năm 2026", Masking.mask("Chuyển 50,000 won vào năm 2026"));
    }

    @Test
    void 중괄호는_괄호로_바꾼다() {
        assertEquals("(test)", Masking.mask("{test}"));
    }
}
