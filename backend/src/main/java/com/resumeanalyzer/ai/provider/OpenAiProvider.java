package com.resumeanalyzer.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.common.exception.AiProviderException;
import com.resumeanalyzer.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * {@link com.resumeanalyzer.ai.AiProvider} implementation backed by the OpenAI
 * Chat Completions API. Registered as a Spring bean named {@code openai} and selected
 * when {@code app.ai.provider=openai}.
 */
@Slf4j
@Component("openai")
public class OpenAiProvider extends AbstractLlmProvider {

    private final WebClient webClient;
    private final AppProperties.Ai.OpenAi config;
    private final AppProperties props;

    public OpenAiProvider(ObjectMapper objectMapper, WebClient.Builder builder, AppProperties properties) {
        super(objectMapper);
        this.props = properties;
        this.config = properties.ai().openai();
        this.webClient = builder
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    protected String complete(List<ChatTurn> conversation) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new AiProviderException("OPENAI_API_KEY is not configured");
        }
        Map<String, Object> body = Map.of(
                "model", config.model(),
                "temperature", 0.4,
                "messages", conversation.stream().map(this::toMessage).toList());
        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(props.ai().requestTimeout());
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI request failed", e);
            throw new AiProviderException("OpenAI request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, String> toMessage(ChatTurn turn) {
        String role = switch (turn.role()) {
            case SYSTEM -> "system";
            case ASSISTANT -> "assistant";
            case USER -> "user";
        };
        return Map.of("role", role, "content", turn.content());
    }
}
