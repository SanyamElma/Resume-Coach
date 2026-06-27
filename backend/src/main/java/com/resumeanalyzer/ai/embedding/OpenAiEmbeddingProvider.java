package com.resumeanalyzer.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.config.AiEngineProperties;
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
 * OpenAI embeddings via {@code /embeddings} ({@code text-embedding-3-small}). The v3 models
 * support a {@code dimensions} parameter, so we request the shared engine dimension to keep
 * vectors interchangeable with the other providers at the storage layer.
 */
@Slf4j
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final String MODEL = "text-embedding-3-small";

    private final WebClient webClient;
    private final AppProperties.Ai.OpenAi config;
    private final ObjectMapper objectMapper;
    private final int dimensions;

    public OpenAiEmbeddingProvider(WebClient.Builder builder, AppProperties appProperties,
                                   AiEngineProperties engineProperties, ObjectMapper objectMapper) {
        this.config = appProperties.ai().openai();
        this.objectMapper = objectMapper;
        this.dimensions = engineProperties.embeddingDimensions();
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
    public String model() {
        return MODEL;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text == null ? "" : text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new AiProviderException("OPENAI_API_KEY is not configured for embeddings");
        }
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "dimensions", dimensions,
                "input", texts);
        try {
            String response = webClient.post().uri("/embeddings")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode data = objectMapper.readTree(response).path("data");
            List<float[]> vectors = new java.util.ArrayList<>(data.size());
            for (JsonNode node : data) {
                vectors.add(toVector(node.path("embedding")));
            }
            return vectors;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI embedding request failed", e);
            throw new AiProviderException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    private float[] toVector(JsonNode array) {
        float[] vec = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            vec[i] = (float) array.get(i).asDouble();
        }
        return vec;
    }
}
