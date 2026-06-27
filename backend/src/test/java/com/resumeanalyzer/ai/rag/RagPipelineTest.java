package com.resumeanalyzer.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.chunk.ChunkingEngine;
import com.resumeanalyzer.ai.config.AiEngineProperties;
import com.resumeanalyzer.ai.embedding.EmbeddingProviderResolver;
import com.resumeanalyzer.ai.embedding.MockEmbeddingProvider;
import com.resumeanalyzer.ai.embedding.Vectors;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.skill.SkillDictionary;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.InMemoryVectorStore;
import com.resumeanalyzer.ai.vector.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end RAG pipeline test (clean → chunk → embed → store → retrieve), fully offline. */
class RagPipelineTest {

    private final AiEngineProperties props =
            new AiEngineProperties("mock", 768, "memory", new AiEngineProperties.Retrieval(5, 0.0));
    private final MockEmbeddingProvider embedding = new MockEmbeddingProvider(props);
    private final EmbeddingProviderResolver resolver =
            new EmbeddingProviderResolver(List.of(embedding), props);
    private final SkillExtractor skillExtractor = new SkillExtractor(new SkillDictionary());
    private final InMemoryVectorStore store = new InMemoryVectorStore();
    private final RagIngestService ingest = new RagIngestService(
            new ResumeCleaner(), new ResumeSectionDetector(),
            new ChunkingEngine(skillExtractor), resolver, store, new ObjectMapper());
    private final Retriever retriever = new Retriever(resolver, store, props);

    private static final String RESUME = """
            John Doe
            Summary
            Backend engineer focused on distributed systems.
            Skills
            Java, Spring Boot, PostgreSQL, Docker, Kubernetes
            Experience
            Acme - Senior Engineer Jan 2019 - Dec 2021
            Built scalable microservices in Java and Spring Boot backed by PostgreSQL.
            Education
            Bachelor of Technology in Computer Science
            """;

    @Test
    void mockEmbedding_isDeterministic_andSemanticallyOrdered() {
        assertThat(embedding.embed("java spring boot")).isEqualTo(embedding.embed("java spring boot"));

        double near = Vectors.cosine(embedding.embed("java spring boot microservices"),
                embedding.embed("java spring boot and docker"));
        double far = Vectors.cosine(embedding.embed("java spring boot microservices"),
                embedding.embed("italian cooking pasta recipes"));
        assertThat(near).isGreaterThan(far);
    }

    @Test
    void ingestThenRetrieve_returnsRelevantChunks_andCachesUnchangedResume() {
        UUID resumeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RagIngestService.IngestResult first = ingest.ingest(resumeId, userId, RESUME);
        assertThat(first.chunkCount()).isGreaterThan(0);
        assertThat(first.cached()).isFalse();

        // Re-ingesting unchanged content is skipped (embedding cache).
        RagIngestService.IngestResult second = ingest.ingest(resumeId, userId, RESUME);
        assertThat(second.cached()).isTrue();

        List<ScoredChunk> hits = retriever.retrieveForResume(
                resumeId, userId, "microservices built with Java and Spring Boot", null);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).content().toLowerCase()).containsAnyOf("microservices", "java", "spring");
        // Retrieval is owner-scoped: a different user sees nothing.
        assertThat(retriever.retrieveForResume(resumeId, UUID.randomUUID(), "java", null)).isEmpty();
    }
}
