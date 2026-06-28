package com.resumeanalyzer.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of versioned prompt templates. All prompts live here (not inline in
 * services) so they are reviewable, versioned, and individually swappable. Every template
 * instructs the model to return strict JSON and to use ONLY the supplied grounded facts —
 * the anti-hallucination contract.
 */
@Component
public class PromptRegistry {

    public static final String ANALYSIS_EXPLAIN = "analysis.explain";
    public static final String INTERVIEW_QUESTIONS = "interview.questions";
    public static final String RESUME_REWRITE = "resume.rewrite";

    private final Map<String, PromptTemplate> templates = new LinkedHashMap<>();

    public PromptRegistry() {
        register(new PromptTemplate(ANALYSIS_EXPLAIN, "v1",
                """
                You are a senior technical recruiter and resume coach. You are given DETERMINISTIC,
                already-computed facts (scores, matched/missing skills) and RETRIEVED excerpts from
                the candidate's actual resume. Your job is ONLY to explain and advise — never to
                recompute scores and never to invent skills, employers, projects, or experience that
                are not present in the retrieved excerpts.
                Respond with ONLY a JSON object, no markdown, matching:
                { "strengths": string[], "weaknesses": string[], "recommendations": string[] }
                Be specific, actionable, and grounded strictly in the provided facts and excerpts.""",
                """
                ATS SCORE: {{atsScore}}
                SUB-SCORES: skills={{skillScore}}, experience={{experienceScore}}, education={{educationScore}}, keywords={{keywordScore}}
                MATCHED SKILLS: {{matchedSkills}}
                MISSING REQUIRED SKILLS: {{missingSkills}}
                CANDIDATE TOTAL EXPERIENCE (years): {{totalYears}}

                RETRIEVED RESUME EXCERPTS:
                {{context}}

                Produce grounded strengths, weaknesses, and recommendations as JSON."""));

        register(new PromptTemplate(INTERVIEW_QUESTIONS, "v1",
                """
                You are a senior interviewer. Using the candidate's retrieved resume excerpts, the
                target role's required skills, and the identified skill gaps, generate tailored
                questions. Never ask generic questions unrelated to the provided context.
                Respond with ONLY a JSON array, no markdown, of objects:
                { "category": "TECHNICAL"|"BEHAVIORAL"|"HR"|"SYSTEM_DESIGN",
                  "difficulty": "EASY"|"MEDIUM"|"HARD", "question": string }""",
                """
                TARGET ROLE SKILLS: {{requiredSkills}}
                CANDIDATE SKILL GAPS: {{missingSkills}}
                RESUME EXCERPTS:
                {{context}}

                Generate exactly {{count}} tailored questions as a JSON array."""));

        register(new PromptTemplate(RESUME_REWRITE, "v1",
                """
                You are a resume-writing expert. Improve the supplied bullet points for ATS and
                impact (quantified results, strong verbs). Use ONLY information present in the
                excerpts — do not fabricate metrics or experience.
                Respond with ONLY a JSON object: { "improvedBullets": string[], "tips": string[] }""",
                """
                TARGET KEYWORDS: {{keywords}}
                RESUME EXCERPTS:
                {{context}}

                Rewrite the bullets and give ATS tips as JSON."""));
    }

    private void register(PromptTemplate template) {
        templates.put(template.id(), template);
    }

    public PromptTemplate get(String id) {
        PromptTemplate template = templates.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Unknown prompt template: " + id);
        }
        return template;
    }
}
