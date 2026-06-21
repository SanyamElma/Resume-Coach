package com.resumeanalyzer.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * Request to generate interview questions. At least one of resume/job is recommended for
 * tailored output; both are optional so a candidate can practise generically.
 */
public record GenerateQuestionsRequest(
        UUID resumeId,
        UUID jobDescriptionId,
        @Min(1) @Max(50) Integer count
) {
    public int effectiveCount() {
        return count == null ? 20 : count;
    }
}
