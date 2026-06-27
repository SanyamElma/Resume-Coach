package com.resumeanalyzer.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the RAG/AI engine ({@code app.ai.engine.*}). Kept separate from the
 * core {@code AppProperties} so the AI layer can evolve independently.
 *
 * <p>A single fixed embedding dimension is shared by every provider (OpenAI v3 is asked to
 * reduce to it; Gemini is native 768; the mock generates it) so the pgvector column type is
 * stable regardless of which provider is active.</p>
 */
@ConfigurationProperties(prefix = "app.ai.engine")
public record AiEngineProperties(
        String embeddingProvider,     // mock | openai | gemini
        int embeddingDimensions,      // shared vector dimension (default 768)
        String vectorStore,           // pgvector | memory
        Retrieval retrieval
) {
    public record Retrieval(int topK, double similarityThreshold) {}

    public AiEngineProperties {
        if (embeddingProvider == null || embeddingProvider.isBlank()) {
            embeddingProvider = "mock";
        }
        if (embeddingDimensions <= 0) {
            embeddingDimensions = 768;
        }
        if (vectorStore == null || vectorStore.isBlank()) {
            vectorStore = "pgvector";
        }
        if (retrieval == null) {
            retrieval = new Retrieval(5, 0.25);
        }
    }
}
