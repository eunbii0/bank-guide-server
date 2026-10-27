package com.example.bankguide.engine;

import java.util.List;

/**
 * 안내 엔진: 1단계 guideEngine.js의 handlePageEnter를 Java로 옮긴 것.
 * Spring이나 DB를 전혀 모르는 순수 로직이라 단위 테스트하기 쉽다.
 * 문구는 이미 고객 언어로 바뀐 상태로 Route에 담겨 들어온다.
 *
 * 핵심 규칙은 하나: "고객이 들어온 화면이 메뉴 지도의 몇 번째 단계인가?"
 *  - 목표 화면       → 안내 종료 (DONE)
 *  - 지도에 있는 화면 → 그 단계의 버튼을 안내 (STEP)
 *  - 지도에 없는 화면 → 경로 이탈, 뒤로가기를 안내 (OFF_PATH)
 */
public final class GuideEngine {

    public record Step(String page, String target, String message) {}

    public record Route(String goalPage, List<Step> steps, String offPathMessage, String doneMessage) {}

    /** 화면 진입 결과. stepIndex는 STEP일 때만 0 이상, 그 외에는 -1. */
    public record Result(Guide guide, int stepIndex) {}

    private GuideEngine() {}

    public static Result onPageEnter(Route route, String page) {
        if (route.goalPage().equals(page)) {
            return new Result(Guide.done(route.doneMessage()), -1);
        }

        List<Step> steps = route.steps();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.page().equals(page)) {
                return new Result(Guide.step(i + 1, steps.size(), step.target(), step.message()), i);
            }
        }

        return new Result(Guide.offPath("btn-back", route.offPathMessage()), -1);
    }
}
