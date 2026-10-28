package com.example.bankguide.handoff;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 상담원 화면 '고객 이동 경로'의 내용 칸을 한국어로 바꾼다 (Spring·DB 없음 → 단위 테스트 가능).
 * DB(guide_event.detail)에는 GuideService가 쓴 형식 그대로 두고, 보여줄 때만 바꾼다.
 * 규칙 요약(HandoffNote)이 detail의 "OFF_PATH"를 세므로 저장 형식은 바꾸지 않는다.
 * 모르는 형식이면 원래 글자를 그대로 돌려준다.
 */
public final class PathNote {

    // GuideService가 쓰는 형식
    private static final Pattern START = Pattern.compile("^(\\S+)(?: (\\d+)개월)? \\((\\w+)\\)$"); // "SAVINGS_OPEN 6개월 (vi)"
    private static final Pattern STEP = Pattern.compile("^STEP (\\d+)/(\\d+)$");                    // "STEP 2/4"
    private static final Pattern OFFER = Pattern.compile("^OFF_PATH x(\\d+)$");                     // "OFF_PATH x3"
    private static final Pattern IDLE = Pattern.compile("^idle (\\d+)s$");                          // "idle 30s"

    private PathNote() {}

    /** taskName: 업무 코드 → 한국어 업무 이름 (모르면 코드 그대로) */
    public static String detailKo(String type, String detail, Function<String, String> taskName) {
        if (detail == null || detail.isBlank()) {
            return "-";
        }
        Matcher m;
        switch (type) {
            case "GUIDE_START" -> {
                if ((m = START.matcher(detail)).matches()) {
                    String term = m.group(2) == null ? "" : " · " + m.group(2) + "개월";
                    return taskName.apply(m.group(1)) + term + " · " + HandoffNote.langKo(m.group(3));
                }
            }
            case "PAGE_ENTER" -> {
                if ((m = STEP.matcher(detail)).matches()) {
                    return m.group(1) + "/" + m.group(2) + "단계 안내";
                }
                if ("OFF_PATH".equals(detail)) return "경로 이탈";
                if ("DONE".equals(detail)) return "목표 화면 도착 (안내 완료)";
            }
            case "OFFER" -> {
                if ((m = OFFER.matcher(detail)).matches()) {
                    return "경로 이탈 " + m.group(1) + "회 → 상담원 연결 제안";
                }
            }
            case "STUCK" -> {
                if ((m = IDLE.matcher(detail)).matches()) {
                    return m.group(1) + "초 동안 멈춤";
                }
            }
            case "HANDOFF" -> {
                return "사유: " + HandoffNote.reasonKo(detail);
            }
            default -> {
                // 그대로
            }
        }
        return detail;
    }
}
