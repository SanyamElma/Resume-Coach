package com.resumeanalyzer.ai.jd;

import java.util.List;

/**
 * Structured representation of a job description, extracted deterministically before any
 * LLM call. This is what skill matching and ATS scoring consume — the JD text itself is
 * never sent wholesale to the model.
 */
public record JobRequirements(
        List<String> requiredSkills,
        List<String> preferredSkills,
        List<String> keywords,
        Integer minYearsExperience,
        Seniority seniority
) {
    public enum Seniority { JUNIOR, MID, SENIOR, LEAD }
}
