package com.example.bankguide.api;

import com.example.bankguide.api.ApiDtos.EventRequest;
import com.example.bankguide.api.ApiDtos.EventResponse;
import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.StartRequest;
import com.example.bankguide.api.ApiDtos.StartResponse;
import com.example.bankguide.api.ApiDtos.StopRequest;
import com.example.bankguide.api.ApiDtos.StuckRequest;
import com.example.bankguide.api.ApiDtos.StuckResponse;
import com.example.bankguide.service.GuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    /** 의도 확인 후 안내 시작 → 세션 id + 첫 안내 */
    @PostMapping("/start")
    public StartResponse start(@RequestBody StartRequest request) {
        return guideService.start(request.intent(), request.page(), request.lang());
    }

    /** 화면에 들어올 때마다 호출 → 다음 안내 (+ 이탈 3회째면 상담원 연결 제안) */
    @PostMapping("/event")
    public EventResponse event(@RequestBody EventRequest request) {
        return guideService.onPageEnter(request.sessionId(), request.page());
    }

    /** 같은 화면에서 오래 멈춰 있음 → 기록 + 상담원 연결 제안 문구 */
    @PostMapping("/stuck")
    public StuckResponse stuck(@RequestBody StuckRequest request) {
        return guideService.stuck(request.sessionId(), request.page(), request.idleSec());
    }

    /** 고객이 '안내 그만하기'를 누름 */
    @PostMapping("/stop")
    public ResponseEntity<Void> stop(@RequestBody StopRequest request) {
        guideService.stop(request.sessionId());
        return ResponseEntity.noContent().build();
    }

    /** 세션의 행동 기록 (DB guide_event 테이블) */
    @GetMapping("/{sessionId}/events")
    public List<EventView> events(@PathVariable String sessionId) {
        return guideService.events(sessionId);
    }
}
