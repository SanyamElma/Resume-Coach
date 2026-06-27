package com.resumeanalyzer.ai.vector;

import java.util.Set;
import java.util.UUID;

/**
 * A similarity-search request with metadata filtering. Nullable fields are ignored when
 * absent, enabling queries like "top-K chunks of resume X in the EXPERIENCE section".
 *
 * @param queryEmbedding the query vector
 * @param topK           max results
 * @param threshold      minimum cosine similarity (0-1)
 * @param userId         restrict to this owner (nullable)
 * @param resumeId       restrict to this resume (nullable)
 * @param sections       restrict to these sections (nullable/empty = any)
 * @param provider       embedding provider that must have produced the stored vectors
 * @param model          embedding model that must have produced the stored vectors
 */
public record VectorSearchRequest(
        float[] queryEmbedding,
        int topK,
        double threshold,
        UUID userId,
        UUID resumeId,
        Set<String> sections,
        String provider,
        String model
) {}
