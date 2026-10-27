package com.example.bankguide.handoff;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/** LLM으로 상담원용 한국어 요약을 만든다. 입력 글은 HandoffNote.toPrompt()가 만든다. */
@Component
public class HandoffSummarizer {

    private static final String SYSTEM_PROMPT = """
            You write a handoff note for a Korean bank call-center agent.
            The customer is a foreigner and may have written in another language.
            Write everything in Korean.

            - "summary": exactly 3 short Korean sentences.
              1) what the customer wants to do,
              2) where they stopped and what they asked (translate their question into Korean),
              3) anything notable such as wrong screens visited or signs of pressure from someone else.
            - "checkpoints": 1 to 3 short Korean items the agent should confirm, specific to this customer's task
              (e.g. a visa or eligibility question, a form field, a missing document).
              Ask whether a third party requested the transfer only when the task is a money transfer
              or the riskFlag is FRAUD_SUSPECTED. Do not add that item for cards, certificates or joining a product.
            - "riskFlag": "FRAUD_SUSPECTED" if anyone seems to be pressuring the customer to move money
              or lend their account, otherwise "NONE".

            Use only facts in the input. Do not guess amounts, names or reasons that are not there.
            Do not say the customer is doing something (such as transferring money) unless they said so.
            Describe what someone else told the customer as what the customer reported, in natural Korean.
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
