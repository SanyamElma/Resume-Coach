package com.resumeanalyzer.ai.vector;

import com.resumeanalyzer.ai.embedding.Vectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link VectorStore} for development, tests, and the offline H2 profile. Performs
 * exact cosine search in Java. Activated when {@code app.ai.engine.vector-store=memory}.
 * Not persistent — production uses {@link PgVectorStore}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.engine.vector-store", havingValue = "memory")
public class InMemoryVectorStore implements VectorStore {

    private final Map<UUID, List<VectorRecord>> byResume = new ConcurrentHashMap<>();

    public InMemoryVectorStore() {
        log.info("VectorStore: in-memory (non-persistent)");
    }

    @Override
    public void replaceResumeChunks(UUID resumeId, List<VectorRecord> records) {
        byResume.put(resumeId, new ArrayList<>(records));
    }

    @Override
    public List<ScoredChunk> search(VectorSearchRequest request) {
        return byResume.values().stream()
                .flatMap(List::stream)
                .filter(r -> matches(r, request))
                .map(r -> new ScoredChunk(r.id(), r.resumeId(), r.section(), r.content(),
                        r.metadataJson(), Vectors.cosine(request.queryEmbedding(), r.embedding())))
                .filter(c -> c.score() >= request.threshold())
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, request.topK()))
                .toList();
    }

    @Override
    public void deleteByResume(UUID resumeId) {
        byResume.remove(resumeId);
    }

    @Override
    public Optional<String> currentContentHash(UUID resumeId) {
        List<VectorRecord> records = byResume.get(resumeId);
        if (records == null || records.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(records.get(0).contentHash());
    }

    private boolean matches(VectorRecord r, VectorSearchRequest req) {
        if (req.resumeId() != null && !req.resumeId().equals(r.resumeId())) {
            return false;
        }
        if (req.userId() != null && !req.userId().equals(r.userId())) {
            return false;
        }
        if (req.provider() != null && !req.provider().equalsIgnoreCase(r.provider())) {
            return false;
        }
        if (req.model() != null && !req.model().equalsIgnoreCase(r.model())) {
            return false;
        }
        if (req.sections() != null && !req.sections().isEmpty() && !req.sections().contains(r.section())) {
            return false;
        }
        return r.embedding().length == req.queryEmbedding().length;
    }
}
