package com.resumeanalyzer.ai.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.chunk.ChunkingEngine;
import com.resumeanalyzer.ai.config.AiEngineProperties;
import com.resumeanalyzer.ai.embedding.EmbeddingProviderResolver;
import com.resumeanalyzer.ai.embedding.MockEmbeddingProvider;
import com.resumeanalyzer.ai.jd.JobRequirementExtractor;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.prompt.PromptRegistry;
import com.resumeanalyzer.ai.provider.MockAiProvider;
import com.resumeanalyzer.ai.rag.RagIngestService;
import com.resumeanalyzer.ai.rag.Retriever;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.skill.SkillDictionary;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.skill.SkillMatcher;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.InMemoryVectorStore;
import com.resumeanalyzer.config.properties.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end interview orchestrator test on the deterministic (mock) path — fully offline. */
class InterviewOrchestratorTest {

    private final AiEngineProperties engineProps =
            new AiEngineProperties("mock", 768, "memory", new AiEngineProperties.Retrieval(5, 0.0));
    private final ObjectMapper mapper = new ObjectMapper();
    private final SkillExtractor skillExtractor = new SkillExtractor(new SkillDictionary());
    private final MockEmbeddingProvider embedding = new MockEmbeddingProvider(engineProps);
    private final EmbeddingProviderResolver embeddingResolver =
            new EmbeddingProviderResolver(List.of(embedding), engineProps);
    private final InMemoryVectorStore store = new InMemoryVectorStore();

    private final AppProperties appProps =
            new AppProperties(null, null, new AppProperties.Ai("mock", null, null, null));
    private final AiProviderResolver aiResolver =
            new AiProviderResolver(Map.of("mock", new MockAiProvider()), appProps);

    private final InterviewOrchestrator orchestrator = new InterviewOrchestrator(
            new ResumeCleaner(), skillExtractor, new JobRequirementExtractor(skillExtractor),
            new SkillMatcher((t, r, th) -> Optional.empty()),
            new RagIngestService(new ResumeCleaner(), new ResumeSectionDetector(),
                    new ChunkingEngine(skillExtractor), embeddingResolver, store, mapper),
            new Retriever(embeddingResolver, store, engineProps),
            aiResolver, new PromptRegistry(), mapper);

    private static final String RESUME = """
            Jane Doe
            Skills
            Java, Spring Boot, PostgreSQL, Docker
            Experience
            Acme - Senior Engineer Jan 2019 - Dec 2021
            Built scalable microservices in Java and Spring Boot backed by PostgreSQL.
            """;

    private static final String JD = """
            Senior Java Engineer. 4+ years with Java, Spring Boot, PostgreSQL and Kubernetes required.
            """;

    @Test
    void generateQuestions_producesTailoredDeterministicSet() {
        List<InterviewQuestion> questions =
                orchestrator.generateQuestions(UUID.randomUUID(), UUID.randomUUID(), RESUME, JD, 5);

        assertThat(questions).hasSize(5);
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.question()).isNotBlank();
            assertThat(q.category()).isIn("TECHNICAL", "BEHAVIORAL", "HR", "SYSTEM_DESIGN");
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
        });
        // Tailored: at least one question references a concrete skill from the resume/JD.
        assertThat(questions).anySatisfy(q ->
                assertThat(q.question().toLowerCase()).containsAnyOf("java", "spring", "postgresql", "kubernetes"));
    }

    @Test
    void generateQuestions_worksWithoutResumeId() {
        List<InterviewQuestion> questions =
                orchestrator.generateQuestions(null, null, "", JD, 3);

        assertThat(questions).hasSize(3);
        assertThat(questions).allSatisfy(q -> assertThat(q.question()).isNotBlank());
    }
}
