package com.example.bankguide.handoff;

import com.example.bankguide.api.ApiDtos.EventView;
import com.example.bankguide.api.ApiDtos.MessageView;
import com.example.bankguide.api.ApiDtos.TimelineView;
import com.example.bankguide.common.Masking;
import com.example.bankguide.common.PageNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 상담원에게 넘길 재료를 정리하는 순수 로직 (Spring·DB 없음 → 단위 테스트 가능).
 *  - toPrompt(): LLM에게 줄 입력 글
 *  - fallback(): LLM이 실패했을 때 규칙으로 만드는 요약
 */
public final class HandoffNote {

    /**
     * 요약에 필요한 재료 한 묶음. events는 챗봇을 시작한 뒤 이 대화에서 남은 모든 안내 기록,
     * timeline은 그 기록을 상담원 화면 '업무 진행 이력'과 같은 한국어 문장으로 바꾼 것
     */
    public record Input(String lang, String intentNameKo, String reason, String currentPage,
                        List<EventView> events, List<MessageView> messages, List<TimelineView> timeline) {

        /** 업무 코드를 한국어로 바꾸지 않은 이력으로 만든다 (테스트용) */
        public Input(String lang, String intentNameKo, String reason, String currentPage,
                     List<EventView> events, List<MessageView> messages) {
            this(lang, intentNameKo, reason, currentPage, events, messages,
                    PathNote.timeline(null, events, code -> code));
        }
    }

    private static final Map<String, String> REASONS = Map.of(
            "ADVICE", "손익·상품 판단 질문",
            "STUCK", "같은 화면에서 오래 멈춤",
            "OFF_PATH", "경로 이탈 반복",
            "FRAUD", "금융사기 의심",
            "CUSTOMER_REQUEST", "고객이 직접 요청");

    private static final Map<String, String> LANGS = Map.of(
            "ko", "한국어", "en", "영어", "vi", "베트남어", "zh", "중국어",
            "uz", "우즈베크어", "ne", "네팔어", "km", "크메르어(캄보디아어)");

    private HandoffNote() {}

    public static String reasonKo(String reason) {
        return REASONS.getOrDefault(reason, reason);
    }

    public static String langKo(String lang) {
        return LANGS.getOrDefault(lang, lang);
    }

    /** 인계 카드 제목 한 줄. 예) "적금 가입 중 경로 이탈 반복", "업무 확인 전 금융사기 의심" */
    public static String headline(String intentNameKo, String reason, String riskFlag) {
        String task = intentNameKo == null ? "업무 확인 전" : intentNameKo + " 중";
        if ("FRAUD".equals(reason) || "FRAUD_SUSPECTED".equals(riskFlag)) {
            return task + " 금융사기 의심";
        }
        return task + " " + switch (reason == null ? "" : reason) {
            case "ADVICE" -> "판단이 필요한 질문";
            case "STUCK" -> "같은 화면에서 멈춤";
            case "OFF_PATH" -> "경로 이탈 반복";
            default -> "상담원 연결 요청";
        };
    }

    /**
     * LLM에게 줄 입력. 고객 원문은 그대로 주되 긴 숫자는 가린다.
     * 행동 기록은 상담원 화면과 같은 한국어 문장으로 주고, 눈에 띄는 신호(이탈·멈춤·안내 변경)는 횟수로 따로 적는다
     */
    public static String toPrompt(Input in) {
        StringBuilder sb = new StringBuilder();
        sb.append("[기본 정보]\n");
        sb.append("고객 언어: ").append(langKo(in.lang())).append('\n');
        sb.append("업무: ").append(in.intentNameKo() == null ? "아직 정해지지 않음" : in.intentNameKo()).append('\n');
        sb.append("연결 사유: ").append(reasonKo(in.reason())).append('\n');
        sb.append("현재 화면: ").append(PageNames.of(in.currentPage())).append('\n');
        sb.append("행동 신호: ").append(signals(in.events())).append('\n');

        sb.append("\n[고객 행동 기록] 챗봇 시작부터 상담원 연결까지, 시간순\n");
        if (in.timeline().isEmpty()) {
            sb.append("- (화면 안내 없이 채팅으로만 상담)\n");
        }
        for (TimelineView t : in.timeline()) {
            sb.append("- ").append(t.time()).append(' ').append(t.text())
                    .append("warn".equals(t.tone()) ? "  ← 확인 필요" : "").append('\n');
        }

        sb.append("\n[대화 기록] 원문, 시간순\n");
        if (in.messages().isEmpty()) {
            sb.append("- (고객이 채팅으로 남긴 말 없음)\n");
        }
        for (MessageView m : in.messages()) {
            sb.append("- ").append(m.time()).append(' ')
                    .append(switch (m.sender()) {
                        case "CUSTOMER" -> "고객";
                        case "AGENT" -> "상담원";
                        case "SYSTEM" -> "시스템";
                        default -> "챗봇";
                    }).append(": ")
                    .append(Masking.mask(m.content())).append('\n');
        }
        return sb.toString();
    }

    /** 경로 이탈 화면 진입. HANDOFF 기록의 detail도 사유 "OFF_PATH"일 수 있어서 화면 진입만 센다 */
    private static boolean isOffPath(EventView e) {
        return "PAGE_ENTER".equals(e.type()) && "OFF_PATH".equals(e.detail());
    }

    /** "안내 2회 시작 ·경로 이탈 3회 · 멈춤 1회 · 안내 중단 1회" (없으면 "특이 사항 없음") */
    static String signals(List<EventView> events) {
        long starts = events.stream().filter(e -> "GUIDE_START".equals(e.type())).count();
        long offPath = events.stream().filter(HandoffNote::isOffPath).count();
        long stuck = events.stream().filter(e -> "STUCK".equals(e.type())).count();
        long stops = events.stream().filter(e -> "GUIDE_STOP".equals(e.type())).count();
        List<String> parts = new ArrayList<>();
        if (starts > 1) parts.add("안내 " + starts + "회 시작 (업무를 바꾸거나 다시 시작함)");
        if (offPath > 0) parts.add("경로 이탈 " + offPath + "회");
        if (stuck > 0) parts.add("멈춤 " + stuck + "회");
        if (stops > 0) parts.add("안내 중단 " + stops + "회");
        return parts.isEmpty() ? "특이 사항 없음" : String.join(" · ", parts);
    }

    /** LLM 없이 규칙으로 만드는 요약. 내용은 단순하지만 사실만 담는다. */
    public static SummaryResult fallback(Input in) {
        List<String> summary = new ArrayList<>();

        String task = in.intentNameKo() == null ? "업무를 정하기 전에" : in.intentNameKo() + " 진행 중";
        summary.add(task + " 상담원 연결 (사유: " + reasonKo(in.reason()) + ").");

        long offPath = in.events().stream().filter(HandoffNote::isOffPath).count();
        long stuck = in.events().stream().filter(e -> "STUCK".equals(e.type())).count();
        StringBuilder where = new StringBuilder("마지막 화면: " + PageNames.of(in.currentPage()));
        if (offPath > 0) where.append(", 경로 이탈 ").append(offPath).append("회");
        if (stuck > 0) where.append(", 멈춤 ").append(stuck).append("회");
        summary.add(where.append('.').toString());

        String last = in.messages().stream()
                .filter(m -> "CUSTOMER".equals(m.sender()))
                .reduce((a, b) -> b)
                .map(MessageView::content)
                .orElse(null);
        if (last == null) {
            summary.add("고객이 채팅으로 남긴 질문은 없음.");
        } else {
            String cut = last.length() > 80 ? last.substring(0, 80) + "…" : last;
            summary.add("고객 마지막 메시지(" + langKo(in.lang()) + " 원문): \"" + Masking.mask(cut) + "\"");
        }

        boolean fraud = "FRAUD".equals(in.reason());
        List<String> checkpoints = fraud
                ? List.of("누가 계좌·송금을 요구했는지 확인", "이미 돈이나 계좌 정보를 넘겼는지 확인")
                : in.intentNameKo() == null
                ? List.of("고객이 원하는 업무 확인")
                : List.of("자금 용도와 필요 시점", "제3자의 해지·송금·계좌 대여 요구 여부");

        return new SummaryResult(summary, checkpoints, fraud ? "FRAUD_SUSPECTED" : "NONE");
    }

    /** LLM 요약을 검사하고 다듬는다. 쓸 수 없으면 null → 호출한 쪽이 fallback을 쓴다. */
    public static SummaryResult sanitize(SummaryResult llm, String reason) {
        return sanitize(llm, reason, null);
    }

    /**
     * intentNameKo: 고객이 하던 업무 이름 (없으면 null).
     * "제3자가 시켰는지" 확인 항목은 이체·해지·사기 의심일 때만 남긴다
     * (LLM이 카드 발급 같은 평범한 상담에도 끼워 넣어서 상담원과 AI 추천 답변에 군더더기가 된다).
     */
    public static SummaryResult sanitize(SummaryResult llm, String reason, String intentNameKo) {
        if (llm == null || llm.summary() == null || llm.summary().isEmpty()) {
            return null;
        }
        List<String> summary = llm.summary().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(Masking::mask)
                .limit(3)
                .toList();
        if (summary.isEmpty()) {
            return null;
        }
        // 사기 의심으로 넘어온 건은 LLM 판단과 상관없이 위험 표시를 유지한다
        String risk = "FRAUD".equals(reason) || "FRAUD_SUSPECTED".equals(llm.riskFlag()) ? "FRAUD_SUSPECTED" : "NONE";
        boolean moneyTask = intentNameKo == null || intentNameKo.contains("이체")
                || intentNameKo.contains("송금") || intentNameKo.contains("해지");
        boolean keepThirdParty = "FRAUD_SUSPECTED".equals(risk) || moneyTask;
        List<String> checkpoints = llm.checkpoints() == null ? List.of()
                : llm.checkpoints().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .filter(s -> keepThirdParty || !(s.contains("제3자") || s.contains("타인")))
                        .limit(3).toList();
        return new SummaryResult(summary, checkpoints, risk);
    }
}
