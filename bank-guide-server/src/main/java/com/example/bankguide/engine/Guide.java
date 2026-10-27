package com.example.bankguide.engine;

/**
 * 화면에 내려보내는 안내 한 건. 1단계의 guide 객체와 같은 모양이다.
 * 예) {"type":"STEP","step":2,"total":4,"target":"btn-deposit","message":"'예금'을 눌러주세요"}
 */
public record Guide(String type, Integer step, Integer total, String target, String message) {

    public static Guide step(int step, int total, String target, String message) {
        return new Guide("STEP", step, total, target, message);
    }

    public static Guide offPath(String target, String message) {
        return new Guide("OFF_PATH", null, null, target, message);
    }

    public static Guide done(String message) {
        return new Guide("DONE", null, null, null, message);
    }
}
