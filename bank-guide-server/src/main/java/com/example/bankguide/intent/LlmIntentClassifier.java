package com.example.bankguide.intent;

import com.example.bankguide.domain.GuideTask;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM 의도 분류기.
 * DB의 업무 목록을 선택지로 주고, 고객 문장이 어느 업무인지 + 어떤 언어인지 +
 * 사람에게 넘겨야 하는지(4단계)를 한 번에 판단하게 한다.
 * 결과는 Spring AI가 IntentResult record로 바로 변환해 준다.
 */
@Component
public class LlmIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            You classify what a bank app customer wants to do.
            Customers are foreigners living in Korea. They may write in any language,
            mix languages, use informal words, or make typos.

            Choose ONLY from these services:
            %s

            Rules for "intent":
            - If exactly one service clearly fits, put its code in "intent" and leave "candidates" empty.
            - If two or more services could fit, set "intent" to "AMBIGUOUS" and list their codes in "candidates".
            - If none fits (including services not in the list, such as loans, exchange rates,
              overseas remittance or opening a new checking account), set "intent" to "UNKNOWN".
            - Never invent a code that is not in the list.
            - Questions about how to do something ("How do I get a credit card?", "Can I open a deposit?")
              are NOT advice requests. Classify them by the service they ask about.
              Examples:
                "How do I get a credit card?" -> intent CREDIT_CARD, escalation NONE
                "How can I open a time deposit?" -> intent TIME_DEPOSIT_OPEN, escalation NONE
                "I want to start an installment savings" -> intent SAVINGS_OPEN, escalation NONE
                "적금 들고 싶어요" -> intent SAVINGS_OPEN, escalation NONE
                "How do I send money to another bank?" -> intent TRANSFER, escalation NONE
                "신용카드는 어떻게 만들어요?" -> intent CREDIT_CARD, escalation NONE
              Only a question that asks for a judgment stays ADVICE, even if it names a service:
                "Which card is better for me?" -> escalation ADVICE
                "Should I close my deposit early?" -> escalation ADVICE
            - If the customer only wants a card but does not say credit or debit, set "intent" to "AMBIGUOUS"
              and list both card codes.
            - Greetings, thanks and small talk ("hello", "thank you", "안녕하세요") are "UNKNOWN".
            - Overseas remittance, opening a new checking (입출금) account and loans are NOT in the list: "UNKNOWN",
              even though they mention sending money or an account.
              Opening an installment savings (적금) or a time deposit (정기예금) IS in the list.
            - Account numbers or ID numbers in the sentence are normal personal data, not a sign of fraud.

            Rules for "language":
            - The ISO 639-1 code of the language the customer wrote in (e.g. ko, en, vi, zh, ne, uz).

            Rules for "escalation" (whether a human agent must take over):
            - "FRAUD" if someone asks the customer to lend, rent or sell their bank account or card,
              to move money to a "safe account", or to send money urgently, including callers claiming
              to be police, prosecutors, immigration officers or bank staff, or job offers that use
              the customer's account. When in doubt between FRAUD and NONE, choose FRAUD.
              A customer who simply asks to send their own money to an account, even with account or
              ID numbers in the sentence, and mentions no outside person asking for it, is NONE.
            - "ADVICE" if the customer asks for a judgment or advice: whether they lose money,
              why the interest is low, which option is better, whether they should do it,
              or a complaint about the product.
            - Otherwise "NONE".
            """;

    private final ChatClient chatClient;

    public LlmIntentClassifier(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public IntentResult classify(String text, List<GuideTask> tasks) {
        String services = tasks.stream()
                .map(t -> "- " + t.getCode() + ": " + t.getDescription())
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(services))
                .user(text)
                .call()
                .entity(IntentResult.class);
    }
}
