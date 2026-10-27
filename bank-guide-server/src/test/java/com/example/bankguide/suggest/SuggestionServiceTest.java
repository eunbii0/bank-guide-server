package com.example.bankguide.suggest;

import com.example.bankguide.domain.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionServiceTest {

    private static final List<ChatMessage> HISTORY = List.of(
            new ChatMessage("c1", "AI", "en", "Connecting you to an agent."),
            new ChatMessage("c1", "AGENT", "ko", "영문 이름을 여권과 똑같이 입력해 주세요.",
                    "Please enter your English name exactly as on your passport."),
            new ChatMessage("c1", "CUSTOMER", "en", "It still fails. My number is 110-234-567890",
                    "여전히 안 됩니다. 제 번호는 110-234-567890"));

    @Test
    void 상담원에게는_고객말의_한국어_번역과_본인_답변_원문을_보여준다() {
        String t = SuggestionService.transcript(HISTORY, true);
        assertTrue(t.contains("Agent (me): 영문 이름을 여권과 똑같이 입력해 주세요."));
        assertTrue(t.contains("Customer: 여전히 안 됩니다."));
        assertFalse(t.contains("It still fails"));
    }

    @Test
    void 고객에게는_상담원말의_고객언어_번역을_보여준다() {
        String t = SuggestionService.transcript(HISTORY, false);
        assertTrue(t.contains("Agent: Please enter your English name exactly as on your passport."));
        assertTrue(t.contains("Customer (me): It still fails."));
    }

    @Test
    void 긴_숫자는_LLM으로_보내기_전에_가린다() {
        String t = SuggestionService.transcript(HISTORY, true);
        assertFalse(t.contains("567890"));
        assertTrue(t.contains("***"));
    }

    @Test
    void 추천문장을_정리한다() {
        List<String> cleaned = SuggestionService.clean(Arrays.asList(
                "1. \"확인해 보겠습니다.\"", "- 확인해 보겠습니다.", "  ", null, "다른 방법을 안내드릴게요.",
                "x".repeat(200), "마지막 문장입니다.", "넘치는 문장입니다."));
        assertEquals(List.of("확인해 보겠습니다.", "다른 방법을 안내드릴게요.", "마지막 문장입니다."), cleaned);
    }

    @Test
    void 입력이_없으면_빈_목록() {
        assertTrue(SuggestionService.clean(null).isEmpty());
    }
}
