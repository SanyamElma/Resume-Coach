package com.resumeanalyzer.ai;

import com.resumeanalyzer.ai.model.AnswerEvaluation;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.InterviewFeedback;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.model.JobAnalysis;
import com.resumeanalyzer.ai.model.ParsedResume;
import com.resumeanalyzer.ai.model.SkillGapResult;

import java.util.List;

/**
 * Abstraction over a generative-AI backend (Strategy pattern).
 *
 * <p>Implementations ({@code OpenAiProvider}, {@code GeminiProvider}, {@code MockAiProvider})
 * encapsulate provider-specific prompt construction and HTTP details, while the rest of
 * the application depends only on this stable, provider-agnostic contract. The active
 * implementation is selected at runtime by {@code AiProviderResolver} based on the
 * {@code app.ai.provider} property — switching providers requires no code changes.</p>
 */
public interface AiProvider {

    /** A stable identifier (e.g. {@code "openai"}, {@code "gemini"}, {@code "mock"}). */
    String name();

    /** Extracts structured fields (skills, education, experience, …) from resume text. */
    ParsedResume analyzeResume(String resumeText);

    /** Structures a free-text job description into required/preferred skills and keywords. */
    JobAnalysis analyzeJobDescription(String jobDescriptionText);

    /** Compares a resume against a job description and produces the skill-gap report. */
    SkillGapResult analyzeSkillGap(String resumeText, String jobDescriptionText);

    /** Generates a set of interview questions tailored to the resume and job description. */
    List<InterviewQuestion> generateQuestions(String resumeText, String jobDescriptionText, int count);

    /** Produces the interviewer's opening message for a new mock interview. */
    String startInterview(String resumeText, String jobDescriptionText);

    /** Evaluates the candidate's latest answer and returns the interviewer's next turn. */
    AnswerEvaluation evaluateAnswer(List<ChatTurn> conversation, String candidateAnswer);

    /** Produces the final aggregated feedback for a completed interview. */
    InterviewFeedback generateFeedback(List<ChatTurn> conversation);

    /**
     * Raw chat completion used by the orchestrator for grounded prompts assembled from the
     * {@link com.resumeanalyzer.ai.prompt.PromptRegistry}. The mock provider returns an empty
     * JSON object (the orchestrator uses deterministic prose when the mock is active).
     */
    String generate(List<ChatTurn> conversation);
}
