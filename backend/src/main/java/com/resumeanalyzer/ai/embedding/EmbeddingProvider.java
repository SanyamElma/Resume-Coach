package com.resumeanalyzer.ai.embedding;

import java.util.List;

/**
 * Abstraction over a text-embedding backend (Strategy pattern, mirroring {@code AiProvider}).
 * Implementations: {@code MockEmbeddingProvider} (offline, deterministic, default),
 * {@code OpenAiEmbeddingProvider}, {@code GeminiEmbeddingProvider}.
 *
 * <p>All implementations emit the same {@link #dimensions()} so embeddings are
 * interchangeable at the storage layer; retrieval is always filtered to the provider+model
 * that produced the stored vectors to avoid mixing incompatible vector spaces.</p>
 */
public interface EmbeddingProvider {

    /** Stable id used for selection and stored alongside each embedding ({@code mock}/{@code openai}/…). */
    String name();

    /** Concrete model identifier, stored with each embedding (e.g. {@code text-embedding-3-small}). */
    String model();

    /** Output vector dimension (shared across providers). */
    int dimensions();

    /** Embeds a single text into a unit-normalised vector. */
    float[] embed(String text);

    /** Embeds many texts; providers may override to batch the upstream call. */
    default List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
