package com.resumeanalyzer.ai.provider;

import com.resumeanalyzer.ai.AiProvider;
import com.resumeanalyzer.ai.model.AnswerEvaluation;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.InterviewFeedback;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.model.JobAnalysis;
import com.resumeanalyzer.ai.model.ParsedResume;
import com.resumeanalyzer.ai.model.SkillGapResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, offline AI provider used as the default ({@code app.ai.provider=mock}).
 *
 * <p>Implements the {@link AiProvider} contract with transparent heuristics — keyword
 * extraction, set overlap for scoring, templated questions/feedback — so the entire
 * platform is fully functional in development and CI without any external API key or cost.
 * Swapping to a real model is a one-line config change.</p>
 */
@Component("mock")
public class MockAiProvider implements AiProvider {

    /** A representative dictionary of skills used for keyword-based extraction and matching. */
    private static final List<String> SKILL_DICTIONARY = List.of(
            "java", "spring", "spring boot", "hibernate", "jpa", "react", "javascript", "typescript",
            "node", "python", "django", "flask", "sql", "postgresql", "mysql", "mongodb", "redis",
            "docker", "kubernetes", "aws", "azure", "gcp", "kafka", "rabbitmq", "rest", "graphql",
            "microservices", "ci/cd", "git", "jenkins", "terraform", "html", "css", "tailwind",
            "redux", "junit", "mockito", "maven", "gradle", "linux", "agile", "scrum", "system design");

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String generate(List<ChatTurn> conversation) {
        // The orchestrator uses deterministic prose when the mock provider is active, so this
        // is never relied upon for content; return an empty JSON object as a safe default.
        return "{}";
    }

    @Override
    public ParsedResume analyzeResume(String resumeText) {
        String text = safe(resumeText);
        return new ParsedResume(
                guessName(text),
                firstMatch(EMAIL, text),
                firstMatch(PHONE, text),
                "Experienced professional with a background aligned to the extracted skill set.",
                extractSkills(text),
                extractSection(text, "education"),
                extractSection(text, "experience"),
                extractSection(text, "certification"),
                extractSection(text, "project"));
    }

    @Override
    public JobAnalysis analyzeJobDescription(String jobDescriptionText) {
        List<String> skills = extractSkills(jobDescriptionText);
        int half = Math.max(1, skills.size() / 2);
        return new JobAnalysis(
                skills.subList(0, Math.min(half, skills.size())),
                skills.size() > half ? skills.subList(half, skills.size()) : List.of(),
                extractSkills(jobDescriptionText),
                extractExperienceYears(jobDescriptionText));
    }

    @Override
    public SkillGapResult analyzeSkillGap(String resumeText, String jobDescriptionText) {
        Set<String> resumeSkills = new LinkedHashSet<>(extractSkills(resumeText));
        Set<String> jobSkills = new LinkedHashSet<>(extractSkills(jobDescriptionText));

        List<String> matched = jobSkills.stream().filter(resumeSkills::contains).toList();
        List<String> missing = jobSkills.stream().filter(s -> !resumeSkills.contains(s)).toList();

        int skillScore = jobSkills.isEmpty() ? 70 : (int) Math.round(100.0 * matched.size() / jobSkills.size());
        int keywordScore = clamp(skillScore + 5);
        int experienceScore = clamp(60 + matched.size() * 4);
        int educationScore = resumeText != null && resumeText.toLowerCase().contains("bachelor") ? 85 : 70;
        int overall = clamp((int) Math.round(skillScore * 0.5 + keywordScore * 0.2
                + experienceScore * 0.2 + educationScore * 0.1));

        List<String> strengths = matched.isEmpty()
                ? List.of("Demonstrates relevant professional experience")
                : matched.stream().limit(6).map(s -> "Strong match on " + s).toList();
        List<String> weaknesses = missing.isEmpty()
                ? List.of("No major gaps detected against the listed requirements")
                : missing.stream().limit(6).map(s -> "Limited evidence of " + s).toList();
        List<String> recommendations = new ArrayList<>();
        missing.stream().limit(4).forEach(s ->
                recommendations.add("Add concrete, quantified examples demonstrating " + s));
        recommendations.add("Mirror key terminology from the job description to improve ATS keyword matching");
        recommendations.add("Lead bullet points with measurable impact (metrics, scale, outcomes)");

        return new SkillGapResult(overall, skillScore, experienceScore, educationScore, keywordScore,
                missing, strengths, weaknesses, recommendations);
    }

    @Override
    public List<InterviewQuestion> generateQuestions(String resumeText, String jobDescriptionText, int count) {
        List<String> skills = extractSkills(jobDescriptionText.isBlank() ? resumeText : jobDescriptionText);
        if (skills.isEmpty()) {
            skills = List.of("your core technical stack", "a recent project", "your problem-solving approach");
        }
        List<InterviewQuestion> questions = new ArrayList<>();
        String[] difficulties = {"EASY", "MEDIUM", "HARD"};
        for (int i = 0; i < count; i++) {
            String skill = skills.get(i % skills.size());
            String difficulty = difficulties[i % difficulties.length];
            questions.add(switch (i % 4) {
                case 0 -> new InterviewQuestion("TECHNICAL", difficulty,
                        "Explain how you have used " + skill + " in a production setting.");
                case 1 -> new InterviewQuestion("BEHAVIORAL", difficulty,
                        "Tell me about a time you faced a challenge involving " + skill + ". How did you handle it?");
                case 2 -> new InterviewQuestion("SYSTEM_DESIGN", difficulty,
                        "Design a system that leverages " + skill + " at scale. Walk me through your approach.");
                default -> new InterviewQuestion("HR", difficulty,
                        "Why are you interested in a role that emphasises " + skill + "?");
            });
        }
        return questions;
    }

    @Override
    public String startInterview(String resumeText, String jobDescriptionText) {
        List<String> skills = extractSkills(jobDescriptionText.isBlank() ? resumeText : jobDescriptionText);
        String focus = skills.isEmpty() ? "your background" : skills.get(0);
        return "Hi, I'm Alex, your interviewer today. Thanks for joining. "
                + "Let's start simple: could you walk me through your experience with " + focus + "?";
    }

    @Override
    public AnswerEvaluation evaluateAnswer(List<ChatTurn> conversation, String candidateAnswer) {
        int length = safe(candidateAnswer).trim().split("\\s+").length;
        int score = clamp(40 + Math.min(length, 120) / 2); // longer, structured answers score higher
        String feedback = length < 15
                ? "Try to elaborate with a concrete example and quantify the impact."
                : "Good detail — consider tightening the structure using the STAR method.";
        long answered = conversation.stream().filter(t -> t.role() == ChatTurn.Role.USER).count();
        String next = answered >= 4
                ? "Thanks, that's helpful. That's all my questions — let's wrap up."
                : "Thanks. Next: tell me about a difficult technical decision you made and its trade-offs.";
        return new AnswerEvaluation(score, feedback, next);
    }

    @Override
    public InterviewFeedback generateFeedback(List<ChatTurn> conversation) {
        List<Integer> answerLengths = conversation.stream()
                .filter(t -> t.role() == ChatTurn.Role.USER)
                .map(t -> safe(t.content()).trim().split("\\s+").length)
                .toList();
        int avg = answerLengths.isEmpty() ? 0
                : answerLengths.stream().mapToInt(Integer::intValue).sum() / answerLengths.size();
        int communication = clamp(55 + Math.min(avg, 80) / 2);
        int technical = clamp(50 + Math.min(avg, 100) / 2);
        int confidence = clamp(60 + Math.min(avg, 60) / 2);
        int overall = (communication + technical + confidence) / 3;
        return new InterviewFeedback(communication, technical, confidence, overall,
                List.of("Provide more quantified, results-oriented examples",
                        "Structure answers with the STAR method",
                        "Deepen explanations of technical trade-offs"),
                "Solid interview overall. Focus on concrete metrics and structured storytelling to stand out.");
    }

    // ---------------------------- heuristics ---------------------------------

    private List<String> extractSkills(String text) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : SKILL_DICTIONARY) {
            if (lower.contains(skill)) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    private List<String> extractSection(String text, String keyword) {
        List<String> lines = new ArrayList<>();
        for (String line : safe(text).split("\\r?\\n")) {
            if (line.toLowerCase(Locale.ROOT).contains(keyword) && line.length() < 200) {
                lines.add(line.trim());
            }
        }
        return lines.isEmpty() ? List.of() : lines;
    }

    private Integer extractExperienceYears(String text) {
        Matcher m = Pattern.compile("(\\d+)\\+?\\s*years").matcher(safe(text).toLowerCase());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private String guessName(String text) {
        String firstLine = safe(text).strip().split("\\r?\\n", 2)[0].trim();
        return firstLine.length() <= 60 && firstLine.matches("[A-Za-z .'-]+") ? firstLine : "";
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(safe(text));
        return m.find() ? m.group().trim() : "";
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
