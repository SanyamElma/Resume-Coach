package com.resumeanalyzer.ai.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.chunk.ChunkingEngine;
import com.resumeanalyzer.ai.config.AiEngineProperties;
import com.resumeanalyzer.ai.embedding.EmbeddingProviderResolver;
import com.resumeanalyzer.ai.embedding.MockEmbeddingProvider;
import com.resumeanalyzer.ai.experience.ExperienceCalculator;
import com.resumeanalyzer.ai.jd.JobRequirementExtractor;
import com.resumeanalyzer.ai.model.SkillGapResult;
import com.resumeanalyzer.ai.prompt.PromptRegistry;
import com.resumeanalyzer.ai.provider.MockAiProvider;
import com.resumeanalyzer.ai.rag.RagIngestService;
import com.resumeanalyzer.ai.rag.Retriever;
import com.resumeanalyzer.ai.scoring.AtsScorer;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.skill.SkillDictionary;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.skill.SkillMatcher;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.InMemoryVectorStore;
import com.resumeanalyzer.config.properties.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end orchestrator test on the deterministic (mock) path — fully offline. */
class ResumeAnalysisOrchestratorTest {

    private final AiEngineProperties engineProps =
            new AiEngineProperties("mock", 768, "memory", new AiEngineProperties.Retrieval(5, 0.0));
    private final ObjectMapper mapper = new ObjectMapper();
    private final SkillExtractor skillExtractor = new SkillExtractor(new SkillDictionary());
    private final MockEmbeddingProvider embedding = new MockEmbeddingProvider(engineProps);
    private final EmbeddingProviderResolver embeddingResolver =
            new EmbeddingProviderResolver(java.util.List.of(embedding), engineProps);
    private final InMemoryVectorStore store = new InMemoryVectorStore();

    private final AppProperties appProps =
            new AppProperties(null, null, new AppProperties.Ai("mock", null, null, null));
    private final AiProviderResolver aiResolver =
            new AiProviderResolver(Map.of("mock", new MockAiProvider()), appProps);

    private final ResumeAnalysisOrchestrator orchestrator = new ResumeAnalysisOrchestrator(
            new ResumeCleaner(), new ResumeSectionDetector(), skillExtractor,
            new JobRequirementExtractor(skillExtractor),
            new SkillMatcher((t, r, th) -> Optional.empty()),
            new ExperienceCalculator(skillExtractor), new AtsScorer(),
            new RagIngestService(new ResumeCleaner(), new ResumeSectionDetector(),
                    new ChunkingEngine(skillExtractor), embeddingResolver, store, mapper),
            new Retriever(embeddingResolver, store, engineProps),
            aiResolver, new PromptRegistry(), mapper);

    private static final String RESUME = """
            Jane Doe
            Skills
            Java, Spring Boot, PostgreSQL, Docker, Kubernetes
            Experience
            Acme - Senior Engineer Jan 2019 - Dec 2021
            Built scalable microservices in Java and Spring Boot backed by PostgreSQL.
            Education
            Bachelor of Technology in Computer Science
            """;

    private static final String JD = """
            Senior Java Engineer. 4+ years with Java, Spring Boot and PostgreSQL required.
            Nice to have: Kubernetes and AWS.
            """;

    @Test
    void analyze_producesDeterministicGroundedReport() {
        SkillGapResult result = orchestrator.analyze(UUID.randomUUID(), UUID.randomUUID(), RESUME, JD);

        assertThat(result.matchScore()).isBetween(0, 100);
        assertThat(result.skillMatchScore()).isGreaterThan(0);
        // All required skills present → AWS (preferred) is the only gap; Java must NOT be missing.
        assertThat(result.missingSkills()).contains("AWS").doesNotContain("Java");
        assertThat(result.strengths()).isNotEmpty();
        assertThat(result.recommendations()).isNotEmpty();
    }
}
