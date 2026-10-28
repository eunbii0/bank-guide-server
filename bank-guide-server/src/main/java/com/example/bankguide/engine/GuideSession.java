package com.example.bankguide.engine;

/** 고객 한 명의 안내 진행 상태: 어떤 업무를, 어떤 언어로, 몇 번째 단계까지 왔는지. */
public class GuideSession {

    private final String sessionId;
    private final String conversationId; // 이 안내를 시작한 대화. 행동 기록(guide_event)에 함께 남긴다
    private final String intent;
    private final String lang;
    private final Integer term; // 예금·적금 가입 기간(개월). 그 밖의 업무는 null
    private int stepIndex;
    private int offPathCount;

    public GuideSession(String sessionId, String intent, String lang) {
        this(sessionId, null, intent, lang, null);
    }

    public GuideSession(String sessionId, String conversationId, String intent, String lang, Integer term) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.intent = intent;
        this.lang = lang;
        this.term = term;
    }

    public void apply(GuideEngine.Result result) {
        if (result.stepIndex() >= 0) {
            stepIndex = result.stepIndex();
        }
        if ("OFF_PATH".equals(result.guide().type())) {
            offPathCount++;
        }
    }

    public String getSessionId() { return sessionId; }
    public String getConversationId() { return conversationId; }
    public String getIntent() { return intent; }
    public String getLang() { return lang; }
    public Integer getTerm() { return term; }
    public int getStepIndex() { return stepIndex; }
    public int getOffPathCount() { return offPathCount; }
}
