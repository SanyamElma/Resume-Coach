package com.resumeanalyzer.ai.model;

/** A generated interview question with its category and difficulty. */
public record InterviewQuestion(
        String category,    // TECHNICAL | BEHAVIORAL | HR | SYSTEM_DESIGN
        String difficulty,  // EASY | MEDIUM | HARD
        String question
) {}
