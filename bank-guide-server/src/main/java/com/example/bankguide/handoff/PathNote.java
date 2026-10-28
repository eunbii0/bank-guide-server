package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.TimelineView;
import com.example.bankguide.common.PageNames;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 상담원 화면 '업무 진행 이력'과 요약 LLM 입력에 쓸 한국어 문장을 만든다 (Spring·DB 없음 → 단위 테스트 가능).
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

    /**
     * 챗봇 시작부터 상담원 연결까지의 이력. 대화 안의 모든 안내(그만두거나 바꾼 안내 포함)를 시간순으로 담는다.
     * firstChatTime: 고객이 챗봇에 처음 말한 시각 (채팅 없이 연결됐으면 null)
     */
    public static List<TimelineView> timeline(String firstChatTime, List<EventView> events,
                                              Function<String, String> taskName) {
        List<TimelineView> out = new ArrayList<>();
        if (firstChatTime != null) {
            out.add(new TimelineView(firstChatTime, "info", "💬", "챗봇 상담 시작"));
        }
        for (EventView e : events) {
            out.add(line(e, taskName));
        }
        return out;
    }

    /** 기록 한 줄 → 이모지 + 상담원이 읽을 문장 */
    public static TimelineView line(EventView e, Function<String, String> taskName) {
        String t = e.time();
        String page = e.page() == null ? null : PageNames.of(e.page());
        String detail = e.detail() == null ? "" : e.detail();
        Matcher m;
        switch (e.type()) {
            case "GUIDE_START" -> {
                return new TimelineView(t, "info", "▶️", "안내 시작 · " + detailKo(e.type(), e.detail(), taskName));
            }
            case "PAGE_ENTER" -> {
                if ((m = STEP.matcher(detail)).matches()) {
                    return new TimelineView(t, "ok", "✅",
                            page + " 진입 · " + m.group(1) + "/" + m.group(2) + "단계");
                }
                if ("OFF_PATH".equals(detail)) {
                    return new TimelineView(t, "warn", "⚠️", page + " 진입 · 안내 경로에서 벗어남");
                }
                if ("DONE".equals(detail)) {
                    return new TimelineView(t, "ok", "🏁", page + " 도착 · 안내 완료");
                }
                return new TimelineView(t, "info", "📄", page + " 진입" + (detail.isEmpty() ? "" : " · " + detail));
            }
            case "OFFER" -> {
                return new TimelineView(t, "warn", "💡", detailKo(e.type(), e.detail(), taskName));
            }
            case "STUCK" -> {
                String where = page == null ? "" : page + "에서 ";
                String how = (m = IDLE.matcher(detail)).matches() ? m.group(1) + "초 동안 멈춤" : "멈춤";
                return new TimelineView(t, "warn", "⏸️", where + how);
            }
            case "GUIDE_STOP" -> {
                return new TimelineView(t, "info", "⏹️", "고객이 안내를 그만둠");
            }
            case "HANDOFF" -> {
                return new TimelineView(t, "agent", "🎧", "상담원 연결 요청 · " + HandoffNote.reasonKo(e.detail()));
            }
            default -> {
                String text = e.type() + (page == null ? "" : " · " + page) + (detail.isEmpty() ? "" : " · " + detail);
                return new TimelineView(t, "info", "•", text);
            }
        }
    }

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
