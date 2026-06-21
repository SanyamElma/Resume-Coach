package com.resumeanalyzer.ai.model;

import java.util.List;

/** Final, aggregated assessment produced when a mock interview is completed. */
public record InterviewFeedback(
        int communicationScore,
        int technicalScore,
        int confidenceScore,
        int overallScore,
        List<String> improvementAreas,
        String summary
) {}
