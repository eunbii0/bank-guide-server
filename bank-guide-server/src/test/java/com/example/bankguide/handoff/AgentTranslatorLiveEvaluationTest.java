package com.example.bankguide.handoff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit opt-in only: calls the paid API with the stored synthetic comparison fixtures. */
@EnabledIfEnvironmentVariable(named = "RUN_TRANSLATION_EVAL", matches = "true")
@SpringBootTest(properties = {
    "spring.ai.openai.base-url=https://api.openai.com",
    "spring.ai.model.chat=openai",
    "spring.datasource.url=jdbc:h2:mem:translation_eval;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.show-sql=false"
})
class AgentTranslatorLiveEvaluationTest {
    @Autowired ChatModel liveModel;

    @Test void recordSyntheticTranslationsAndGuardDecisions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path directory = Path.of("build/translation-evaluation");
        Files.createDirectories(directory);
        Map<String, JsonNode> cases = new LinkedHashMap<>();
        try (var input = getClass().getResourceAsStream("/translation-evaluation-cases.json")) {
            if (input == null) throw new IllegalStateException("Synthetic translation fixtures missing");
            mapper.readTree(input).forEach(fixture -> cases.put(fixture.path("id").asText(), fixture));
        }
        if (cases.isEmpty()) throw new IllegalStateException("No synthetic translation fixtures");
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> calls = new ArrayList<>();
        ChatModel recording = new ChatModel() {
            @Override public ChatResponse call(Prompt prompt) {
                long start = System.nanoTime();
                ChatResponse response = liveModel.call(prompt);
                Map<String, Object> call = new LinkedHashMap<>();
                call.put("request", prompt.getUserMessage().getText());
                call.put("raw_output", response.getResult().getOutput().getText());
                call.put("seconds", (System.nanoTime() - start) / 1_000_000_000.0);
                call.put("model", response.getMetadata().getModel());
                call.put("finish_reason", response.getResult().getMetadata().getFinishReason());
                calls.add(call);
                return response;
            }
        };
        AgentTranslator translator = new AgentTranslator(ChatClient.builder(recording));
        for (JsonNode fixture : cases.values()) {
            calls.clear();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("case", fixture);
            try {
                String text = fixture.path("text").asText();
                String lang = fixture.path("lang").asText();
                String translated = "out".equals(fixture.path("direction").asText())
                        ? translator.translate(text, lang) : translator.toKorean(text, lang);
                result.put("status", "DELIVERED");
                result.put("delivered_text", translated);
            } catch (Exception e) {
                // Never save exception bodies, credentials or server details in an evaluation artifact.
                result.put("status", "WITHHELD");
                result.put("error_type", e.getClass().getSimpleName());
                if (e.getMessage() != null && e.getMessage().startsWith("TRANSLATION_VALIDATION_FAILED")) {
                    result.put("validation_error", e.getMessage());
                }
            }
            result.put("calls", new ArrayList<>(calls));
            results.add(result);
            Files.writeString(directory.resolve("guarded-results.json"), mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(Map.of("results", results, "note", "Synthetic AI-authored cases, one run, not native-speaker evaluation")));
        }
    }
}
