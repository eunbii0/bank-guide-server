package com.example.bankguide.engine;

import com.example.bankguide.api.ApiDtos.RouteStepView;
import com.example.bankguide.api.ApiDtos.RouteView;
import com.example.bankguide.common.PageNames;
import com.example.bankguide.engine.GuideEngine.Result;
import com.example.bankguide.engine.GuideEngine.Route;
import com.example.bankguide.engine.GuideEngine.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * 상담원 화면 '경로 예측'을 만드는 순수 로직 (Spring·DB 없음 → 단위 테스트 가능).
 * 업무 경로(안내 단계 화면들 + 목표 화면)를 한 줄로 늘어놓고, 고객이 어디까지 왔는지와
 * 다음에 갈 화면·누를 버튼을 표시한다. 다음 버튼은 고객 화면 하이라이트와 같은 엔진 결과를 쓴다.
 */
public final class RoutePreview {

    private RoutePreview() {}

    /**
     * result: GuideEngine.onPageEnter(route, page)의 결과.
     * visited: 지금까지 고객이 들어간 화면 (오래된 순). 경로 밖이면 마지막으로 지나간 경로 화면으로 돌아가라고 예측한다
     */
    public static RouteView of(Route route, Result result, String page, List<String> visited) {
        List<String> pages = new ArrayList<>(route.steps().stream().map(Step::page).toList());
        pages.add(route.goalPage());

        Guide guide = result.guide();
        int current;
        int next;
        String action;
        switch (guide.type()) {
            case "STEP" -> {
                current = result.stepIndex();
                next = current + 1;
                action = guide.message();
            }
            case "DONE" -> {
                current = pages.size() - 1;
                next = -1;
                action = "목표 화면에 도착했습니다 (안내 완료)";
            }
            default -> { // OFF_PATH: 마지막으로 지나간 경로 화면으로 돌아가야 한다
                current = -1;
                next = Math.max(0, lastOnRoute(pages, visited));
                action = "뒤로가기를 눌러 '" + PageNames.of(pages.get(next)) + "' 화면으로 돌아가기";
            }
        }

        String target = guide.target();
        // 가입 기간을 모르면 기간 버튼 id가 "btn-term-"로 비어 있다 → 버튼은 짚지 않고 말로만 안내
        if (target != null && target.endsWith("-")) {
            target = null;
            action = "원하는 가입 기간 버튼을 고르도록 안내";
        }

        int doneUntil = current >= 0 ? current : next;
        List<RouteStepView> steps = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            String state = i == current ? "current"
                    : i == next ? "next"
                    : i < doneUntil ? "done"
                    : "todo";
            steps.add(new RouteStepView(pages.get(i), PageNames.of(pages.get(i)), state));
        }
        String offPage = "OFF_PATH".equals(guide.type()) ? PageNames.of(page) : null;
        return new RouteView(guide.type(), steps, target, action, offPage);
    }

    /** visited 중 경로에 있는 마지막 화면의 위치 (없으면 -1) */
    private static int lastOnRoute(List<String> pages, List<String> visited) {
        for (int i = visited.size() - 1; i >= 0; i--) {
            int at = pages.indexOf(visited.get(i));
            if (at >= 0) {
                return at;
            }
        }
        return -1;
    }
}
