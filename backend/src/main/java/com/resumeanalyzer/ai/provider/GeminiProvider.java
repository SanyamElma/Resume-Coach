package com.resumeanalyzer.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.common.exception.AiProviderException;
import com.resumeanalyzer.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link com.resumeanalyzer.ai.AiProvider} implementation backed by Google's Gemini
 * {@code generateContent} API. Registered as bean {@code gemini}; selected when
 * {@code app.ai.provider=gemini}.
 *
 * <p>Gemini has no dedicated "system" role, so system instructions are folded into the
 * first user turn — a common, well-supported pattern.</p>
 */
@Slf4j
@Component("gemini")
public class GeminiProvider extends AbstractLlmProvider {

    private final WebClient webClient;
    private final AppProperties.Ai.Gemini config;
    private final AppProperties props;

    public GeminiProvider(ObjectMapper objectMapper, WebClient.Builder builder, AppProperties properties) {
        super(objectMapper);
        this.props = properties;
        this.config = properties.ai().gemini();
        this.webClient = builder
                .baseUrl(config.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    protected String complete(List<ChatTurn> conversation) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new AiProviderException("GEMINI_API_KEY is not configured");
        }
        Map<String, Object> body = Map.of("contents", toContents(conversation));
        try {
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", config.apiKey())
                            .build(config.model()))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(props.ai().requestTimeout());
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asText();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini request failed", e);
            throw new AiProviderException("Gemini request failed: " + e.getMessage(), e);
        }
    }

    /** Maps conversation turns to Gemini's {@code contents} array, merging system prompts. */
    private List<Map<String, Object>> toContents(List<ChatTurn> conversation) {
        List<Map<String, Object>> contents = new ArrayList<>();
        StringBuilder pendingSystem = new StringBuilder();
        for (ChatTurn turn : conversation) {
            if (turn.role() == ChatTurn.Role.SYSTEM) {
                pendingSystem.append(turn.content()).append("\n\n");
                continue;
            }
            String text = turn.content();
            if (!pendingSystem.isEmpty() && turn.role() == ChatTurn.Role.USER) {
                text = pendingSystem + text;
                pendingSystem.setLength(0);
            }
            String role = turn.role() == ChatTurn.Role.ASSISTANT ? "model" : "user";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", text))));
        }
        if (!pendingSystem.isEmpty()) {
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", pendingSystem.toString()))));
        }
        return contents;
    }
}
