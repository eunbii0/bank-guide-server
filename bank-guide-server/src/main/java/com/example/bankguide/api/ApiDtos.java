package com.example.bankguide.api;

import com.example.bankguide.engine.Guide;

import java.util.List;

/** API 요청·응답 모양. 화면(React)과 주고받는 JSON이 이 record들과 1:1로 대응한다. */
public final class ApiDtos {

    private ApiDtos() {}

    // ---------- 채팅 ----------

    /**
     * POST /api/chat  {"conversationId":"...","text":"I want to close my deposit early"}
     * conversationId는 화면이 처음 열릴 때 만든 값. 같은 고객의 대화를 하나로 묶는다.
     * customerId는 데모용 가상 고객 (A, B, C). 없으면 A로 본다.
     */
    public record ChatRequest(String conversationId, String text, String customerId) {}

    /**
     * 분류 결과.
     *  - 확실할 때: intent=DEPOSIT_CANCEL, confirmText=확인 질문
     *  - 애매할 때: intent=AMBIGUOUS, message=되묻는 문구, options=후보 버튼
     *  - 모를 때:   intent=UNKNOWN, message=안내 문구
     *  - 사람이 필요할 때(4단계): intent=ADVICE 또는 FRAUD, message=연결 제안 또는 사기 경고
     *  - 챗봇 첫 메뉴: intent=MENU, message=인사, options=메뉴 버튼
     *  - 가입 자격 확인이 필요한 업무: intent=VERIFY, message=고객 정보 확인 질문, category=CARD 또는 JOIN
     * lang은 고객 언어, source는 LLM / KEYWORD / CHOICE / MENU, escalation은 NONE / ADVICE / FRAUD
     */
    public record ChatResponse(String intent, String confirmText, String lang,
                               String message, List<Option> options, String source, String escalation,
                               String category) {

        public ChatResponse(String intent, String confirmText, String lang,
                            String message, List<Option> options, String source, String escalation) {
            this(intent, confirmText, lang, message, options, source, escalation, null);
        }
    }

    /** 되묻기 선택지 버튼 하나 (메뉴 버튼도 같은 모양) */
    public record Option(String intent, String name) {}

    /** POST /api/chat/choose  {"conversationId":"...","intent":"DEPOSIT_CANCEL","lang":"vi","customerId":"A"} */
    public record ChooseRequest(String conversationId, String intent, String lang, String customerId) {}

    /** POST /api/chat/menu  {"conversationId":"...","customerId":"A","lang":null}  lang이 없으면 고객 선호 언어 */
    public record MenuRequest(String conversationId, String customerId, String lang) {}

    // ---------- 고객 정보·가입 자격 (데모용 가상 규칙) ----------

    /** GET /api/customers 의 한 줄 (시연 패널에서 고객 바꾸기용) */
    public record CustomerView(String customerId, String name, String nationality, String visaType,
                               String stayExpiry, String lang) {}

    /** POST /api/eligibility  confirmed: 고객 정보 확인 질문에 [네]면 true, [아니요]면 false */
    public record EligibilityRequest(String conversationId, String customerId, String category,
                                     String lang, boolean confirmed) {}

    /**
     * status=CHECKED: products에 상품별 판단 결과. 불가 상품이 있으면 offer에 상담원 연결 제안 문구
     * status=MISMATCH: 정보가 다르다고 답함 → message(체류정보 갱신 안내) + offer("상담원을 연결해 드릴까요?")
     * offer가 있으면 화면은 [상담원 연결] 버튼을 띄우고, 누르면 /api/handoff를 reason=CUSTOMER_REQUEST로 부른다.
     */
    public record EligibilityResponse(String status, String lang, String message, String notice,
                                      List<ProductView> products, String offer) {}

    /** 상품 하나의 판단 결과 (문구는 모두 고객 언어) */
    public record ProductView(String productCode, String intent, String name, boolean eligible,
                              List<String> reasons, String note, List<TermView> terms, AltView alternative) {}

    /** 예금·적금 가입 기간 하나 */
    public record TermView(int months, boolean available, String reason) {}

    /** 불가일 때 추천하는 상품 */
    public record AltView(String productCode, String intent, String name, String message) {}

    // ---------- 안내 ----------

    /**
     * POST /api/guide/start  {"intent":"DEPOSIT_CANCEL","page":"home","lang":"en","term":null}
     * term: 예금·적금 가입 기간(개월). 안내 문구와 하이라이트 버튼(btn-term-6)에 들어간다.
     */
    public record StartRequest(String intent, String page, String lang, Integer term) {}

    /** {"sessionId":"...","guide":{...}} */
    public record StartResponse(String sessionId, Guide guide) {}

    /** POST /api/guide/event  {"sessionId":"...","page":"product"} */
    public record EventRequest(String sessionId, String page) {}

    /** 화면 진입 결과. offer는 경로 이탈이 3번째일 때만 들어오는 상담원 연결 제안 문구 (4단계) */
    public record EventResponse(Guide guide, String offer) {}

    /** POST /api/guide/stuck  {"sessionId":"...","page":"deposit-cancel","idleSec":30} (4단계) */
    public record StuckRequest(String sessionId, String page, int idleSec) {}

    /** {"offer":"어려운 부분이 있으신가요? ..."} */
    public record StuckResponse(String offer) {}

    /** POST /api/guide/stop  {"sessionId":"..."} */
    public record StopRequest(String sessionId) {}

    /** GET /api/guide/{sessionId}/events 의 한 줄 */
    public record EventView(String time, String type, String page, String detail) {}

    // ---------- 상담원 연결 (4단계) ----------

    /**
     * POST /api/handoff
     * reason: ADVICE(판단 질문), STUCK(멈춤), OFF_PATH(이탈 반복), FRAUD(사기 의심), CUSTOMER_REQUEST
     */
    public record HandoffRequest(String conversationId, String sessionId, String reason,
                                 String page, String lang, String intent) {}

    /** {"ticketNo":"T-20261026-0001","queue":"FOREIGN","message":"상담원에게 연결하고 있어요..."} */
    public record HandoffResponse(String ticketNo, String queue, String message) {}

    /** 상담원 화면에 보일 티켓 (5단계에서 상담원 페이지가 이걸 그대로 쓴다) */
    public record TicketView(String ticketNo, String queue, String status, String reason,
                             String lang, String intentName, String currentPage,
                             List<String> summary, List<String> checkpoints, String riskFlag,
                             String summarySource, List<MessageView> messages, String createdAt) {}

    /** 원문 대화 한 줄 */
    public record MessageView(String time, String sender, String lang, String content) {}
}
