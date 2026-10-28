package com.example.bankguide.engine;

import com.example.bankguide.api.ApiDtos.RouteStepView;
import com.example.bankguide.api.ApiDtos.RouteView;
import com.example.bankguide.engine.GuideEngine.Route;
import com.example.bankguide.engine.GuideEngine.Step;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoutePreviewTest {

    // 적금 가입 경로: 홈 → 상품가입 메뉴 → 기간 선택 → 가입 내용 확인 → 가입 완료
    private static Route savings(String termTarget) {
        return new Route("join-done", List.of(
                new Step("home", "btn-join", "아래 '상품가입'을 눌러주세요"),
                new Step("join", "btn-join-savings", "'적금'을 눌러주세요"),
                new Step("savings-join", termTarget, "'6개월'을 눌러주세요"),
                new Step("join-confirm", "btn-join-submit", "'가입하기'를 눌러주세요")),
                "다른 화면이에요. 뒤로가기를 눌러주세요", "완료");
    }

    private static RouteView preview(Route route, String page, List<String> visited) {
        return RoutePreview.of(route, GuideEngine.onPageEnter(route, page), page, visited);
    }

    private static List<String> states(RouteView v) {
        return v.steps().stream().map(RouteStepView::state).toList();
    }

    @Test
    void 경로_위에서는_지나온_화면_지금_화면_다음_화면과_누를_버튼을_알려준다() {
        RouteView v = preview(savings("btn-term-6"), "join", List.of("home", "join"));

        assertEquals("STEP", v.status());
        assertEquals(List.of("done", "current", "next", "todo", "todo"), states(v));
        assertEquals("btn-join-savings", v.nextTarget());
        assertEquals("'적금'을 눌러주세요", v.nextAction());
        assertNull(v.offPage());
    }

    @Test
    void 경로_밖이면_마지막으로_지나간_경로_화면으로_돌아가라고_예측한다() {
        RouteView v = preview(savings("btn-term-6"), "loan", List.of("home", "join", "loan"));

        assertEquals("OFF_PATH", v.status());
        assertEquals(List.of("done", "next", "todo", "todo", "todo"), states(v));
        assertEquals("btn-back", v.nextTarget());
        assertEquals("뒤로가기를 눌러 '상품가입 메뉴' 화면으로 돌아가기", v.nextAction());
        assertEquals("대출", v.offPage());
    }

    @Test
    void 목표_화면이면_모두_지나온_것으로_본다() {
        RouteView v = preview(savings("btn-term-6"), "join-done", List.of("home", "join-done"));

        assertEquals("DONE", v.status());
        assertEquals(List.of("done", "done", "done", "done", "current"), states(v));
    }

    @Test
    void 가입_기간을_모르면_기간_버튼을_짚지_않고_말로_안내한다() {
        RouteView v = preview(savings("btn-term-"), "savings-join", List.of("savings-join"));

        assertNull(v.nextTarget());
        assertEquals("원하는 가입 기간 버튼을 고르도록 안내", v.nextAction());
    }
}
