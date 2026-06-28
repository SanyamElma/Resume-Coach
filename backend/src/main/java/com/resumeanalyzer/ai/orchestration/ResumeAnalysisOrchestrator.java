package com.resumeanalyzer.ai.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.experience.ExperienceCalculator;
import com.resumeanalyzer.ai.experience.ExperienceResult;
import com.resumeanalyzer.ai.jd.JobRequirementExtractor;
import com.resumeanalyzer.ai.jd.JobRequirements;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.SkillGapResult;
import com.resumeanalyzer.ai.prompt.PromptRegistry;
import com.resumeanalyzer.ai.prompt.PromptTemplate;
import com.resumeanalyzer.ai.rag.RagIngestService;
import com.resumeanalyzer.ai.rag.Retriever;
import com.resumeanalyzer.ai.security.PromptSanitizer;
import com.resumeanalyzer.ai.scoring.AtsBreakdown;
import com.resumeanalyzer.ai.scoring.AtsScorer;
import com.resumeanalyzer.ai.scoring.AtsSignals;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.section.ResumeSections;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.skill.SkillMatchResult;
import com.resumeanalyzer.ai.skill.SkillMatcher;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The AI orchestrator for resume-vs-JD analysis. It composes the modular pipeline:
 *
 * <pre>clean → section → deterministic extract/match/score → RAG retrieve → (grounded) LLM explain</pre>
 *
 * <p>All numbers are computed deterministically (never by the LLM). The LLM is used only to
 * turn those grounded facts + retrieved resume excerpts into human-readable strengths,
 * weaknesses, and recommendations — and only when a real provider is configured; otherwise a
 * deterministic explanation is produced. Output conforms to the existing {@link SkillGapResult}
 * contract, so no API/DTO change is needed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisOrchestrator {

    private final ResumeCleaner cleaner;
    private final ResumeSectionDetector sectionDetector;
    private final SkillExtractor skillExtractor;
    private final JobRequirementExtractor jdExtractor;
    private final SkillMatcher skillMatcher;
    private final ExperienceCalculator experienceCalculator;
    private final AtsScorer atsScorer;
    private final RagIngestService ragIngestService;
    private final Retriever retriever;
    private final AiProviderResolver aiProviderResolver;
    private final PromptRegistry promptRegistry;
    private final PromptSanitizer promptSanitizer;
    private final GroundedLlmExecutor llmExecutor;
    private final ObjectMapper objectMapper;

    private static final String OPERATION = "analysis.explain";

    public SkillGapResult analyze(UUID resumeId, UUID userId, String resumeText, String jdText) {
        String cleaned = cleaner.clean(resumeText);
        ResumeSections sections = sectionDetector.detect(cleaned);

        Set<String> resumeSkills = skillExtractor.extract(cleaned);
        JobRequirements jd = jdExtractor.extract(jdText);
        SkillMatchResult skillMatch = skillMatcher.match(resumeSkills, jd.requiredSkills(), jd.preferredSkills());
        ExperienceResult experience = experienceCalculator.calculate(cleaned);

        int experienceScore = experienceMatch(experience, jd);
        int keywordScore = keywordCoverage(resumeSkills, jd.keywords());
        AtsBreakdown ats = atsScorer.score(
                new AtsSignals(skillMatch.score(), experienceScore, keywordScore, sections, cleaned));

        // Best-effort RAG grounding: ingest (cached) then retrieve sanitized relevant excerpts.
        RagContext context = retrieveContext(resumeId, userId, jdText, resumeText);

        Prose prose = explain(ats, skillMatch, experience, context);

        return new SkillGapResult(
                ats.overall(), ats.skills(), ats.experience(), ats.education(), ats.keywords(),
                missingSkills(skillMatch), prose.strengths(), prose.weaknesses(), prose.recommendations());
    }

    // --------------------------- deterministic math --------------------------

    private int experienceMatch(ExperienceResult experience, JobRequirements jd) {
        double total = experience.totalYears();
        if (jd.minYearsExperience() == null) {
            return clamp((int) Math.round(50 + total * 8));
        }
        double ratio = total / Math.max(1, jd.minYearsExperience());
        return clamp((int) Math.round(Math.min(1.0, ratio) * 100));
    }

    private int keywordCoverage(Set<String> resumeSkills, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 70;
        }
        long matched = keywords.stream().filter(resumeSkills::contains).count();
        return (int) Math.round(100.0 * matched / keywords.size());
    }

    private List<String> missingSkills(SkillMatchResult match) {
        List<String> missing = new ArrayList<>(match.missingRequired());
        match.missingPreferred().stream().filter(s -> !missing.contains(s)).forEach(missing::add);
        return missing;
    }

    // ------------------------------ RAG context ------------------------------

    /** Retrieved grounding context: sanitized excerpt text plus retrieval observability stats. */
    private record RagContext(String text, int chunkCount, double topSimilarity) {
        static final RagContext EMPTY = new RagContext("", 0, 0.0);
    }

    private RagContext retrieveContext(UUID resumeId, UUID userId, String jdText, String resumeText) {
        try {
            ragIngestService.ingest(resumeId, userId, resumeText);
            List<ScoredChunk> chunks = retriever.retrieveForResume(resumeId, userId,
                    jdText == null || jdText.isBlank() ? "relevant experience and skills" : jdText, null);
            if (chunks.isEmpty()) {
                return RagContext.EMPTY;
            }
            // Sanitize each excerpt against prompt injection before it ever reaches the model.
            String text = chunks.stream()
                    .map(c -> promptSanitizer.sanitize(c.content()).sanitized())
                    .collect(Collectors.joining("\n---\n"));
            double topSimilarity = chunks.stream().mapToDouble(ScoredChunk::score).max().orElse(0.0);
            return new RagContext(text, chunks.size(), topSimilarity);
        } catch (Exception e) {
            log.warn("RAG grounding unavailable, proceeding without retrieved context: {}", e.getMessage());
            return RagContext.EMPTY;
        }
    }

    // ------------------------ explanation (LLM or det.) -----------------------

    private record Prose(List<String> strengths, List<String> weaknesses, List<String> recommendations) {}

    private Prose explain(AtsBreakdown ats, SkillMatchResult match, ExperienceResult experience, RagContext context) {
        if (!"mock".equals(aiProviderResolver.current().name())) {
            try {
                return llmExplain(ats, match, experience, context);
            } catch (Exception e) {
                log.warn("LLM explanation failed, falling back to deterministic prose: {}", e.getMessage());
            }
        }
        llmExecutor.recordFallback(OPERATION, context.chunkCount(), context.topSimilarity());
        return deterministicProse(ats, match, experience);
    }

    private Prose llmExplain(AtsBreakdown ats, SkillMatchResult match, ExperienceResult experience, RagContext context)
            throws Exception {
        PromptTemplate template = promptRegistry.get(PromptRegistry.ANALYSIS_EXPLAIN);
        String user = template.renderUser(Map.of(
                "atsScore", String.valueOf(ats.overall()),
                "skillScore", String.valueOf(ats.skills()),
                "experienceScore", String.valueOf(ats.experience()),
                "educationScore", String.valueOf(ats.education()),
                "keywordScore", String.valueOf(ats.keywords()),
                "matchedSkills", String.join(", ", match.matchedRequired()),
                "missingSkills", String.join(", ", missingSkills(match)),
                "totalYears", String.valueOf(experience.totalYears()),
                "context", context.text().isBlank() ? "(no excerpts retrieved)" : context.text()));

        String raw = llmExecutor.execute(OPERATION, template,
                List.of(ChatTurn.system(template.system()), ChatTurn.user(user)),
                context.chunkCount(), context.topSimilarity());
        JsonNode json = objectMapper.readTree(stripFences(raw));
        return new Prose(
                toList(json.get("strengths")), toList(json.get("weaknesses")), toList(json.get("recommendations")));
    }

    private Prose deterministicProse(AtsBreakdown ats, SkillMatchResult match, ExperienceResult experience) {
        List<String> strengths = new ArrayList<>();
        match.matchedRequired().stream().limit(6).forEach(s -> strengths.add("Strong match on required skill: " + s));
        if (experience.totalYears() > 0) {
            strengths.add("Demonstrates ~%.1f years of relevant experience".formatted(experience.totalYears()));
        }
        if (strengths.isEmpty()) {
            strengths.add("Resume shows relevant professional background");
        }

        List<String> weaknesses = new ArrayList<>();
        match.missingRequired().stream().limit(6).forEach(s -> weaknesses.add("Missing required skill: " + s));
        if (ats.experience() < 60) {
            weaknesses.add("Experience appears below the role's stated requirement");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("No major gaps detected against the listed requirements");
        }

        List<String> recommendations = new ArrayList<>();
        match.missingRequired().stream().limit(4).forEach(s ->
                recommendations.add("Add concrete, quantified evidence of " + s));
        if (ats.keywords() < 70) {
            recommendations.add("Mirror more terminology from the job description to improve ATS keyword matching");
        }
        recommendations.add("Lead bullet points with measurable impact (metrics, scale, outcomes)");
        return new Prose(strengths, weaknesses, recommendations);
    }

    private List<String> toList(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> values.add(n.asText()));
        }
        return values;
    }

    private String stripFences(String raw) {
        String s = raw == null ? "{}" : raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("(?s)^```(?:json)?", "").replaceAll("```\\s*$", "").trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : "{}";
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
