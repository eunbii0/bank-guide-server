package com.example.bankguide.engine;

/** 고객 한 명의 안내 진행 상태: 어떤 업무를, 어떤 언어로, 몇 번째 단계까지 왔는지. */
public class GuideSession {

    private final String sessionId;
    private final String intent;
    private final String lang;
    private int stepIndex;
    private int offPathCount;

    public GuideSession(String sessionId, String intent, String lang) {
        this.sessionId = sessionId;
        this.intent = intent;
        this.lang = lang;
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
    public String getIntent() { return intent; }
    public String getLang() { return lang; }
    public int getStepIndex() { return stepIndex; }
    public int getOffPathCount() { return offPathCount; }
}
