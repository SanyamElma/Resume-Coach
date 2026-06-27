package com.resumeanalyzer.ai.vector;

import java.util.UUID;

/**
 * A chunk plus its embedding, ready to persist in the vector store. Provider+model are
 * stored so retrieval never mixes vectors from incompatible embedding spaces.
 */
public record VectorRecord(
        UUID id,
        UUID resumeId,
        UUID userId,
        String section,
        int chunkIndex,
        String content,
        String metadataJson,
        float[] embedding,
        String provider,
        String model,
        String contentHash
) {}
