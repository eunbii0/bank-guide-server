package com.example.bankguide.suggest;

import com.example.bankguide.suggest.SuggestionService.SuggestionList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    /**
     * 대화 맥락에서 만든 추천 문장.
     * role=agent 이면 상담원이 보낼 한국어 답변, role=customer 이면 고객 언어로 된 고객의 다음 말.
     * 추천이 없으면 suggestions가 빈 목록이니 화면은 고정 예시를 보여준다.
     */
    @GetMapping("/tickets/{ticketNo}/suggestions")
    public SuggestionList suggestions(@PathVariable String ticketNo, @RequestParam String role) {
        return suggestionService.suggest(ticketNo, role);
    }
}
