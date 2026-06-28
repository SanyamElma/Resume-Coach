package com.resumeanalyzer.ai.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.jd.JobRequirementExtractor;
import com.resumeanalyzer.ai.jd.JobRequirements;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.prompt.PromptRegistry;
import com.resumeanalyzer.ai.prompt.PromptTemplate;
import com.resumeanalyzer.ai.rag.RagIngestService;
import com.resumeanalyzer.ai.rag.Retriever;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.skill.SkillMatchResult;
import com.resumeanalyzer.ai.skill.SkillMatcher;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import com.resumeanalyzer.ai.vector.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI orchestrator for the interview features. Produces <em>tailored</em> interview questions by
 * composing the same modular pipeline used for analysis:
 *
 * <pre>clean → deterministic JD/skill extraction + gap analysis → RAG retrieve → (grounded) LLM</pre>
 *
 * <p>The set of skills/gaps a question targets is computed deterministically from the actual
 * resume and JD text — never invented by the model. When a real LLM provider is configured it
 * phrases questions grounded strictly in retrieved resume excerpts; otherwise a deterministic,
 * still-tailored question set is produced. Output conforms to the existing
 * {@link InterviewQuestion} contract, so no API/DTO change is needed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewOrchestrator {

    private final ResumeCleaner cleaner;
    private final SkillExtractor skillExtractor;
    private final JobRequirementExtractor jdExtractor;
    private final SkillMatcher skillMatcher;
    private final RagIngestService ragIngestService;
    private final Retriever retriever;
    private final AiProviderResolver aiProviderResolver;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Generates {@code count} tailored questions for a candidate against an (optional) target role.
     *
     * @param resumeId   the resume being interviewed on (nullable → no RAG grounding)
     * @param userId     the owning user (nullable when no resume)
     * @param resumeText the parsed resume text (may be blank)
     * @param jdText     the target job description (may be blank → resume-only focus)
     */
    public List<InterviewQuestion> generateQuestions(
            UUID resumeId, UUID userId, String resumeText, String jdText, int count) {

        String cleanedResume = cleaner.clean(resumeText);
        Set<String> resumeSkills = skillExtractor.extract(cleanedResume);
        JobRequirements jd = jdExtractor.extract(jdText);

        SkillMatchResult match = skillMatcher.match(resumeSkills, jd.requiredSkills(), jd.preferredSkills());

        // Skills the candidate genuinely has (probe depth) and gaps (probe coverage/learning).
        List<String> strengths = new ArrayList<>(match.matchedRequired());
        List<String> gaps = new ArrayList<>(match.missingRequired());
        // Fall back to the resume's own skills when there is no JD to match against.
        List<String> focusSkills = new ArrayList<>(new LinkedHashSet<>(
                strengths.isEmpty() ? new ArrayList<>(resumeSkills) : strengths));

        String context = retrieveContext(resumeId, userId, jd, jdText);

        if (!"mock".equals(aiProviderResolver.current().name())) {
            try {
                return llmQuestions(jd, gaps, context, count);
            } catch (Exception e) {
                log.warn("LLM question generation failed, falling back to deterministic set: {}", e.getMessage());
            }
        }
        return deterministicQuestions(focusSkills, gaps, count);
    }

    // ------------------------------ RAG context ------------------------------

    private String retrieveContext(UUID resumeId, UUID userId, JobRequirements jd, String jdText) {
        if (resumeId == null || userId == null) {
            return "";
        }
        try {
            ragIngestService.ingest(resumeId, userId, jdText == null ? "" : jdText);
        } catch (Exception ignored) {
            // ingest is best-effort; retrieval below may still hit previously stored chunks
        }
        try {
            String query = jd.requiredSkills().isEmpty()
                    ? (jdText == null || jdText.isBlank() ? "relevant experience and skills" : jdText)
                    : String.join(", ", jd.requiredSkills());
            List<ScoredChunk> chunks = retriever.retrieveForResume(resumeId, userId, query, null);
            return chunks.stream().map(ScoredChunk::content).collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.warn("RAG grounding unavailable for interview questions: {}", e.getMessage());
            return "";
        }
    }

    // --------------------------- LLM (grounded) ------------------------------

    private List<InterviewQuestion> llmQuestions(JobRequirements jd, List<String> gaps, String context, int count)
            throws Exception {
        PromptTemplate template = promptRegistry.get(PromptRegistry.INTERVIEW_QUESTIONS);
        String user = template.renderUser(Map.of(
                "requiredSkills", String.join(", ", jd.requiredSkills()),
                "missingSkills", gaps.isEmpty() ? "(none)" : String.join(", ", gaps),
                "context", context.isBlank() ? "(no excerpts retrieved)" : context,
                "count", String.valueOf(count)));

        String raw = aiProviderResolver.current().generate(List.of(
                ChatTurn.system(template.system()), ChatTurn.user(user)));
        JsonNode array = objectMapper.readTree(stripToArray(raw));

        List<InterviewQuestion> questions = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode node : array) {
                String q = text(node, "question");
                if (q.isBlank()) {
                    continue;
                }
                questions.add(new InterviewQuestion(
                        normalizeCategory(text(node, "category")),
                        normalizeDifficulty(text(node, "difficulty")),
                        q));
            }
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("LLM returned no usable questions");
        }
        return questions.size() > count ? questions.subList(0, count) : questions;
    }

    // --------------------- deterministic (still tailored) --------------------

    private List<InterviewQuestion> deterministicQuestions(List<String> focusSkills, List<String> gaps, int count) {
        List<String> pool = new ArrayList<>(focusSkills);
        for (String g : gaps) {
            if (!pool.contains(g)) {
                pool.add(g);
            }
        }
        if (pool.isEmpty()) {
            pool = new ArrayList<>(List.of(
                    "your core technical stack", "a recent project", "your problem-solving approach"));
        }

        Set<String> gapSet = new LinkedHashSet<>(gaps);
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};
        List<InterviewQuestion> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String skill = pool.get(i % pool.size());
            String difficulty = difficulties[i % difficulties.length];
            boolean isGap = gapSet.contains(skill);
            questions.add(switch (i % 4) {
                case 0 -> new InterviewQuestion("TECHNICAL", difficulty, isGap
                        ? "The role expects " + skill + ", which isn't prominent on your resume. How would you ramp up, and where have you used something similar?"
                        : "Walk me through a production system where you used " + skill + ". What were the trade-offs?");
                case 1 -> new InterviewQuestion("BEHAVIORAL", difficulty,
                        "Tell me about a time " + skill + " was central to solving a difficult problem. What was your specific contribution?");
                case 2 -> new InterviewQuestion("SYSTEM_DESIGN", difficulty,
                        "Design a system that relies on " + skill + " at scale. Walk me through the architecture and failure modes.");
                default -> new InterviewQuestion("HR", difficulty,
                        "How do you keep your " + skill + " skills current, and how does that fit this role?");
            });
        }
        return questions;
    }

    // -------------------------------- helpers --------------------------------

    private String normalizeCategory(String raw) {
        String c = raw == null ? "" : raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (c) {
            case "TECHNICAL", "BEHAVIORAL", "HR", "SYSTEM_DESIGN" -> c;
            default -> "TECHNICAL";
        };
    }

    private String normalizeDifficulty(String raw) {
        String d = raw == null ? "" : raw.trim().toUpperCase();
        return switch (d) {
            case "EASY", "MEDIUM", "HARD" -> d;
            default -> "MEDIUM";
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText().trim();
    }

    private String stripToArray(String raw) {
        String s = raw == null ? "[]" : raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("(?s)^```(?:json)?", "").replaceAll("```\\s*$", "").trim();
        }
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : "[]";
    }
}
