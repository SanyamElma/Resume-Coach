package com.resumeanalyzer.ai.rag;

import com.resumeanalyzer.ai.config.AiEngineProperties;
import com.resumeanalyzer.ai.embedding.EmbeddingProvider;
import com.resumeanalyzer.ai.embedding.EmbeddingProviderResolver;
import com.resumeanalyzer.ai.vector.ScoredChunk;
import com.resumeanalyzer.ai.vector.VectorSearchRequest;
import com.resumeanalyzer.ai.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Retrieval half of the RAG pipeline: embeds the query with the active provider and runs a
 * filtered top-K similarity search. Retrieval is always scoped to the embedding provider+model
 * that produced the stored vectors, preventing cross-space contamination.
 */
@Service
@RequiredArgsConstructor
public class Retriever {

    private final EmbeddingProviderResolver embeddingResolver;
    private final VectorStore vectorStore;
    private final AiEngineProperties properties;

    /** Retrieves the most relevant chunks of a specific resume for a free-text query. */
    public List<ScoredChunk> retrieveForResume(UUID resumeId, UUID userId, String query, Set<String> sections) {
        return retrieve(query, userId, resumeId, sections,
                properties.retrieval().topK(), properties.retrieval().similarityThreshold());
    }

    public List<ScoredChunk> retrieve(String query, UUID userId, UUID resumeId,
                                      Set<String> sections, int topK, double threshold) {
        EmbeddingProvider provider = embeddingResolver.current();
        float[] queryEmbedding = provider.embed(query);
        VectorSearchRequest request = new VectorSearchRequest(
                queryEmbedding, topK, threshold, userId, resumeId, sections,
                provider.name(), provider.model());
        return vectorStore.search(request);
    }
}
