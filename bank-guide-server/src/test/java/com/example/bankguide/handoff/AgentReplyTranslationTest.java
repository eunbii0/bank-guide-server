package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.AgentPoll;
import com.example.bankguide.domain.ChatMessage;
import com.example.bankguide.domain.HandoffTicket;
import com.example.bankguide.domain.HandoffTicketRepository;
import com.example.bankguide.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 상담원 답장 번역이 실패하면 보내지 않고 알리고(422), 상담원이 고르면 그대로 보내며
 * 고객 화면에는 번역 없이 온 답장이라고 표시되는지 확인한다. 번역기는 가짜로 바꾼다 (LLM 호출 없음).
 */
@SpringBootTest
class AgentReplyTranslationTest {

    @Autowired
    private HandoffService handoffService;
    @Autowired
    private HandoffTicketRepository ticketRepository;
    @Autowired
    private ConversationService conversationService;
    @MockitoBean
    private AgentTranslator translator;

    @Test
    void 번역이_실패하면_보내지_않고_TRANSLATION_FAILED로_알린다() {
        ticket("T-TR-1", "c-tr-1", "vi");
        when(translator.translate(anyString(), anyString(), any(TranslationContext.class))).thenThrow(new IllegalStateException("LLM down"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> handoffService.reply("T-TR-1", "버튼을 눌러 주세요", false));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
        assertEquals(HandoffService.TRANSLATION_FAILED, e.getReason());
        assertTrue(conversationService.history("c-tr-1").isEmpty());
    }

    @Test
    void 그대로_보내면_번역_없이_저장하고_고객_화면에_표시한다() {
        ticket("T-TR-2", "c-tr-2", "vi");

        handoffService.reply("T-TR-2", "Please tap the blue button.", true);

        AgentPoll poll = handoffService.poll("T-TR-2", 0);
        assertEquals(1, poll.messages().size());
        assertEquals("Please tap the blue button.", poll.messages().get(0).text());
        assertTrue(poll.messages().get(0).untranslated());
        // 상담원 화면 대화에는 번역 없이 보냈다는 시스템 안내가 남는다
        List<ChatMessage> history = conversationService.history("c-tr-2");
        assertEquals("SYSTEM", history.get(history.size() - 1).getSender());
    }

    @Test
    void 번역되면_고객_언어로_가고_표시가_없다() {
        ticket("T-TR-3", "c-tr-3", "vi");
        when(translator.translate(anyString(), anyString(), any(TranslationContext.class))).thenReturn("Vui lòng nhấn nút.");

        handoffService.reply("T-TR-3", "버튼을 눌러 주세요", false);

        AgentPoll poll = handoffService.poll("T-TR-3", 0);
        assertEquals("Vui lòng nhấn nút.", poll.messages().get(0).text());
        assertFalse(poll.messages().get(0).untranslated());
    }

    @Test
    void 한국어_고객은_번역하지_않아도_표시가_없다() {
        ticket("T-TR-4", "c-tr-4", "ko");

        handoffService.reply("T-TR-4", "버튼을 눌러 주세요", false);

        assertFalse(handoffService.poll("T-TR-4", 0).messages().get(0).untranslated());
    }

    @Test
    void customerTranslationFailurePreservesTheOriginalWithoutInventingKorean() {
        ticket("T-TR-5", "c-tr-5", "vi");
        when(translator.toKorean(anyString(), anyString(), any(TranslationContext.class)))
                .thenThrow(new IllegalStateException("TRANSLATION_VALIDATION_FAILED"));
        var message = handoffService.customerMessage("T-TR-5", "Please explain again.");
        assertEquals("Please explain again.", message.content());
        org.junit.jupiter.api.Assertions.assertNull(message.translated());
        assertEquals("Please explain again.", conversationService.history("c-tr-5").get(0).getContent());
    }

    private void ticket(String ticketNo, String conversationId, String lang) {
        ticketRepository.save(new HandoffTicket(ticketNo, conversationId, null, "FOREIGN", "CUSTOMER_REQUEST",
                lang, null, "home", "요약", "", "NONE", "RULE", null));
    }
}
