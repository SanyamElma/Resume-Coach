package com.resumeanalyzer.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.config.AiEngineProperties;
import com.resumeanalyzer.common.exception.AiProviderException;
import com.resumeanalyzer.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Google Gemini embeddings via {@code text-embedding-004} (native 768-dim), matching the
 * shared engine dimension. Uses the same API key configuration as the Gemini LLM provider.
 */
@Slf4j
@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final String MODEL = "text-embedding-004";

    private final WebClient webClient;
    private final AppProperties.Ai.Gemini config;
    private final ObjectMapper objectMapper;
    private final int dimensions;

    public GeminiEmbeddingProvider(WebClient.Builder builder, AppProperties appProperties,
                                   AiEngineProperties engineProperties, ObjectMapper objectMapper) {
        this.config = appProperties.ai().gemini();
        this.objectMapper = objectMapper;
        this.dimensions = engineProperties.embeddingDimensions();
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
    public String model() {
        return MODEL;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new AiProviderException("GEMINI_API_KEY is not configured for embeddings");
        }
        Map<String, Object> body = Map.of(
                "model", "models/" + MODEL,
                "content", Map.of("parts", java.util.List.of(Map.of("text", text == null ? "" : text))));
        try {
            String response = webClient.post()
                    .uri(uri -> uri.path("/models/{model}:embedContent")
                            .queryParam("key", config.apiKey())
                            .build(MODEL))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode values = objectMapper.readTree(response).path("embedding").path("values");
            float[] vec = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vec[i] = (float) values.get(i).asDouble();
            }
            return vec;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini embedding request failed", e);
            throw new AiProviderException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }
}
