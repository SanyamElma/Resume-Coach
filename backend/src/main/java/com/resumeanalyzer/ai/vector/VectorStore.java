package com.resumeanalyzer.ai.vector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction over a vector database. Implementations: {@code InMemoryVectorStore}
 * (dev/test/offline) and {@code PgVectorStore} (PostgreSQL + pgvector, production).
 *
 * <p>Callers depend only on this contract, so switching the backing store is a config
 * change — mirroring the provider-abstraction approach used elsewhere in the codebase.</p>
 */
public interface VectorStore {

    /** Replaces all chunks for a resume with the supplied records (idempotent re-ingest). */
    void replaceResumeChunks(UUID resumeId, List<VectorRecord> records);

    /** Cosine top-K similarity search with metadata filtering and a similarity threshold. */
    List<ScoredChunk> search(VectorSearchRequest request);

    /** Removes all chunks for a resume. */
    void deleteByResume(UUID resumeId);

    /** Content hash of the currently-stored chunks for a resume, if any (embedding-cache key). */
    Optional<String> currentContentHash(UUID resumeId);
}
