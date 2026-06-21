package com.resumeanalyzer.ai.model;

/**
 * The AI interviewer's evaluation of a single candidate answer plus the follow-up message
 * (next question or closing remark) to send back in the conversation.
 */
public record AnswerEvaluation(
        int score,              // 0-100 for the answer just given
        String feedback,        // short critique of the answer
        String nextMessage      // the interviewer's next turn (question or wrap-up)
) {}
