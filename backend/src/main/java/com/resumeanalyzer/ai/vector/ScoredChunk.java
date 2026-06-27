package com.resumeanalyzer.ai.vector;

import java.util.UUID;

/** A chunk returned from similarity search, with its cosine similarity score (0-1). */
public record ScoredChunk(
        UUID id,
        UUID resumeId,
        String section,
        String content,
        String metadataJson,
        double score
) {}
