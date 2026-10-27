package com.example.bankguide.api;

import com.example.bankguide.api.ApiDtos.ChatRequest;
import com.example.bankguide.api.ApiDtos.ChatResponse;
import com.example.bankguide.api.ApiDtos.ChooseRequest;
import com.example.bankguide.intent.IntentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final IntentService intentService;

    public ChatController(IntentService intentService) {
        this.intentService = intentService;
    }

    /** 고객 문장(어느 언어든) → 업무 코드 + 고객 언어 응답 (사기 의심·판단 질문이면 상담원 연결 제안) */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return intentService.classify(request.conversationId(), request.text());
    }

    /** 애매해서 되물었을 때 고객이 선택지 버튼을 누름 */
    @PostMapping("/choose")
    public ChatResponse choose(@RequestBody ChooseRequest request) {
        return intentService.choose(request.conversationId(), request.intent(), request.lang());
    }
}
