package com.resumeanalyzer.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProvider;
import com.resumeanalyzer.ai.model.AnswerEvaluation;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.model.InterviewFeedback;
import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.model.JobAnalysis;
import com.resumeanalyzer.ai.model.ParsedResume;
import com.resumeanalyzer.ai.model.SkillGapResult;
import com.resumeanalyzer.ai.support.AiPrompts;
import com.resumeanalyzer.ai.support.JsonExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Template implementation of {@link AiProvider} for chat-completion style LLMs.
 *
 * <p>Encapsulates prompt assembly and JSON parsing so concrete providers only implement
 * {@link #complete(List)} — the single provider-specific HTTP call. This keeps the OpenAI
 * and Gemini classes tiny and confines vendor differences to one method.</p>
 */
public abstract class AbstractLlmProvider implements AiProvider {

    protected final ObjectMapper objectMapper;

    protected AbstractLlmProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Performs the actual model call and returns the raw assistant text. */
    protected abstract String complete(List<ChatTurn> conversation);

    @Override
    public ParsedResume analyzeResume(String resumeText) {
        String raw = complete(List.of(
                ChatTurn.system(AiPrompts.RESUME_SYSTEM),
                ChatTurn.user(AiPrompts.truncate(resumeText))));
        return JsonExtractor.parse(objectMapper, raw, ParsedResume.class);
    }

    @Override
    public JobAnalysis analyzeJobDescription(String jobDescriptionText) {
        String raw = complete(List.of(
                ChatTurn.system(AiPrompts.JD_SYSTEM),
                ChatTurn.user(AiPrompts.truncate(jobDescriptionText))));
        return JsonExtractor.parse(objectMapper, raw, JobAnalysis.class);
    }

    @Override
    public SkillGapResult analyzeSkillGap(String resumeText, String jobDescriptionText) {
        String raw = complete(List.of(
                ChatTurn.system(AiPrompts.SKILL_GAP_SYSTEM),
                ChatTurn.user(AiPrompts.skillGapUser(resumeText, jobDescriptionText))));
        return JsonExtractor.parse(objectMapper, raw, SkillGapResult.class);
    }

    @Override
    public List<InterviewQuestion> generateQuestions(String resumeText, String jobDescriptionText, int count) {
        String raw = complete(List.of(
                ChatTurn.system(AiPrompts.QUESTIONS_SYSTEM),
                ChatTurn.user(AiPrompts.questionsUser(resumeText, jobDescriptionText, count))));
        return JsonExtractor.parse(objectMapper, raw, new TypeReference<List<InterviewQuestion>>() {});
    }

    @Override
    public String startInterview(String resumeText, String jobDescriptionText) {
        return complete(List.of(
                ChatTurn.system(AiPrompts.INTERVIEW_SYSTEM),
                ChatTurn.user(("Here is the candidate resume:\n%s\n\nTarget role:\n%s\n\n"
                        + "Introduce yourself as the interviewer and ask your first question.")
                        .formatted(AiPrompts.truncate(resumeText), AiPrompts.truncate(jobDescriptionText)))));
    }

    @Override
    public AnswerEvaluation evaluateAnswer(List<ChatTurn> conversation, String candidateAnswer) {
        List<ChatTurn> turns = new ArrayList<>();
        turns.add(ChatTurn.system(AiPrompts.EVALUATE_SYSTEM));
        turns.addAll(conversation);
        turns.add(ChatTurn.user(candidateAnswer));
        return JsonExtractor.parse(objectMapper, complete(turns), AnswerEvaluation.class);
    }

    @Override
    public InterviewFeedback generateFeedback(List<ChatTurn> conversation) {
        List<ChatTurn> turns = new ArrayList<>();
        turns.add(ChatTurn.system(AiPrompts.FEEDBACK_SYSTEM));
        turns.addAll(conversation);
        turns.add(ChatTurn.user("Produce the final assessment JSON now."));
        return JsonExtractor.parse(objectMapper, complete(turns), InterviewFeedback.class);
    }
}
