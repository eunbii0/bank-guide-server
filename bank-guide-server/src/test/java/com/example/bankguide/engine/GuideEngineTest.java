package com.example.bankguide.engine;

import com.example.bankguide.engine.GuideEngine.Route;
import com.example.bankguide.engine.GuideEngine.Step;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideEngineTest {

    private final Route route = new Route("deposit-done", List.of(
            new Step("home", "btn-product", "Tap '상품관리' (My Products) below"),
            new Step("product", "btn-deposit", "Tap '예금' (Deposits)"),
            new Step("deposit-list", "btn-cancel", "Tap '중도해지' (Early termination) on the deposit you want to close"),
            new Step("deposit-cancel", "btn-cancel-confirm", "Check the details, then tap '해지 신청' (Request termination)")),
            "This is a different screen. Tap the back button (‹) at the top left.",
            "Guidance for Early deposit termination is complete.");

    @Test
    void 지도에_있는_화면이면_그_단계의_버튼을_안내한다() {
        Guide guide = GuideEngine.onPageEnter(route, "product").guide();

        assertEquals("STEP", guide.type());
        assertEquals(2, guide.step());
        assertEquals(4, guide.total());
        assertEquals("btn-deposit", guide.target());
        assertEquals("Tap '예금' (Deposits)", guide.message());
    }

    @Test
    void 지도에_없는_화면이면_뒤로가기를_안내한다() {
        Guide guide = GuideEngine.onPageEnter(route, "loan").guide();

        assertEquals("OFF_PATH", guide.type());
        assertEquals("btn-back", guide.target());
    }

    @Test
    void 목표_화면에_도착하면_안내를_끝낸다() {
        Guide guide = GuideEngine.onPageEnter(route, "deposit-done").guide();

        assertEquals("DONE", guide.type());
    }

    @Test
    void 경로_이탈_횟수를_센다() {
        GuideSession session = new GuideSession("s1", "DEPOSIT_CANCEL", "en");
        session.apply(GuideEngine.onPageEnter(route, "product"));
        session.apply(GuideEngine.onPageEnter(route, "loan"));

        assertEquals(1, session.getStepIndex());
        assertEquals(1, session.getOffPathCount());
    }
}
