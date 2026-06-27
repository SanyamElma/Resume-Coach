package com.resumeanalyzer.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.chunk.Chunk;
import com.resumeanalyzer.ai.chunk.ChunkingEngine;
import com.resumeanalyzer.ai.embedding.EmbeddingProvider;
import com.resumeanalyzer.ai.embedding.EmbeddingProviderResolver;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.section.ResumeSections;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.VectorRecord;
import com.resumeanalyzer.ai.vector.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingest half of the RAG pipeline: clean → section → semantic chunk → embed → store.
 *
 * <p>Embeddings are cached by a content hash that includes the active provider+model: if the
 * resume text and embedding backend are unchanged, ingestion is skipped entirely — satisfying
 * "do not regenerate embeddings if the resume has not changed".</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIngestService {

    private final ResumeCleaner cleaner;
    private final ResumeSectionDetector sectionDetector;
    private final ChunkingEngine chunkingEngine;
    private final EmbeddingProviderResolver embeddingResolver;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public record IngestResult(int chunkCount, boolean cached) {}

    public IngestResult ingest(UUID resumeId, UUID userId, String rawText) {
        EmbeddingProvider provider = embeddingResolver.current();
        String cleaned = cleaner.clean(rawText);
        String contentHash = hash(cleaned + "|" + provider.name() + "|" + provider.model());

        if (vectorStore.currentContentHash(resumeId).filter(contentHash::equals).isPresent()) {
            log.debug("RAG ingest skipped for resume {} (unchanged)", resumeId);
            return new IngestResult(0, true);
        }

        ResumeSections sections = sectionDetector.detect(cleaned);
        List<Chunk> chunks = chunkingEngine.chunk(sections);
        if (chunks.isEmpty()) {
            vectorStore.replaceResumeChunks(resumeId, List.of());
            return new IngestResult(0, false);
        }

        List<float[]> embeddings = provider.embedBatch(chunks.stream().map(Chunk::content).toList());

        List<VectorRecord> records = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            records.add(new VectorRecord(UUID.randomUUID(), resumeId, userId, c.section(), c.index(),
                    c.content(), metadata(c), embeddings.get(i), provider.name(), provider.model(), contentHash));
        }
        vectorStore.replaceResumeChunks(resumeId, records);
        log.info("RAG ingested {} chunks for resume {} via {}/{}", records.size(), resumeId,
                provider.name(), provider.model());
        return new IngestResult(records.size(), false);
    }

    private String metadata(Chunk chunk) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "section", chunk.section(),
                    "skills", chunk.skills()));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
