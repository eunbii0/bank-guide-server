package com.example.bankguide.handoff;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** LLM으로 상담원용 한국어 요약을 만든다. 입력 글은 HandoffNote.toPrompt()가 만든다. */
@Component
public class HandoffSummarizer {

    private static final String SYSTEM_PROMPT = """
            You write a handoff note for a Korean bank call-center agent who is about to take over a chat
            from our mobile-banking guide chatbot. The agent reads it in a few seconds before saying hello,
            so it must tell them what happened and what to do first. Write everything in Korean.

            # Input
            - [기본 정보]: customer language, task, why they were connected, current screen,
              and 행동 신호 (counts of going off the guided path, stopping, restarting or quitting guides).
            - [고객 행동 기록]: everything the customer did since they opened the chatbot, in time order,
              including guides they quit or switched away from. Lines marked "← 확인 필요" are trouble signals.
              "안내 경로에서 벗어남" = they opened a screen that is not on the guided path.
              "멈춤" = no tap for that many seconds. "안내 완료" = they reached the goal screen.
              More than one "안내 시작" = they changed or restarted the task; mention it only if it matters.
            - [대화 기록]: the original chat. Customer lines may be in a foreign language; translate their meaning.

            # How to read it
            Find the point where the customer got stuck: the last step completed before the trouble signals,
            the screen where they went off the path or stopped, and the question they asked around then.
            The latest events matter most. Earlier guides that ended normally are background only.

            # Output (JSON)
            - "summary": exactly 3 Korean sentences, each under 70 characters, formal ending (~했습니다/~입니다).
              1) what the customer wanted and how far they got (last completed step or screen),
              2) where and how they got stuck (off-path screens, stopping, quitting) and what they asked,
                 with their question translated into Korean,
              3) why they asked for an agent, plus anything notable (changed task, signs of pressure from
                 someone else). If nothing is notable, state the connection reason only.
            - "checkpoints": 1 to 3 recommended next actions for the agent, most urgent first.
              Each is a short Korean noun phrase under 25 characters ending in an action word such as
              "확인", "안내", "재안내" (e.g. "가입 기간 선택 버튼 위치 안내", "여권 영문 이름 입력 형식 확인").
              Tie each one to a concrete screen, field or question from the input, not generic advice.
              Ask whether a third party requested the transfer only when the task is a money transfer
              or the riskFlag is FRAUD_SUSPECTED. Do not add that item for cards, certificates or joining a product.
            - "riskFlag": "FRAUD_SUSPECTED" if anyone seems to be pressuring the customer to move money
              or lend their account, otherwise "NONE".

            # Rules
            Use only facts in the input. Do not guess amounts, names, error causes or reasons that are not there.
            Do not say the customer is doing something (such as transferring money) unless they said so.
            Do not say a transaction was completed unless the record shows the goal screen was reached.
            Describe what someone else told the customer as what the customer reported, in natural Korean.
            Use the Korean screen names exactly as written in the input.
            Never write account numbers or phone numbers.
            """;

    private final ChatClient chatClient;

    public HandoffSummarizer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public SummaryResult summarize(HandoffNote.Input input) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(HandoffNote.toPrompt(input))
                .call()
                .entity(SummaryResult.class);
    }
}
