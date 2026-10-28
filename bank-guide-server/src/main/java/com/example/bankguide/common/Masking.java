package com.example.bankguide.common;

/** 외부 LLM으로 보내기 전에 개인정보로 보이는 부분을 가린다. (3단계 IntentService.mask를 옮김) */
public final class Masking {

    private Masking() {}

    /**
     * 계좌번호·전화번호처럼 보이는 6자리 이상 숫자를 *** 로 바꾼다.
     * 중괄호는 Spring AI 프롬프트 템플릿 문법과 겹치므로 괄호로 바꾼다.
     */
    public static String mask(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\d[\\d\\- ]{4,}\\d", "***")
                .replace('{', '(')
                .replace('}', ')');
    }
}
