package com.resumeanalyzer.ai.model;

import java.util.List;

/** Structured representation of a job description. */
public record JobAnalysis(
        List<String> requiredSkills,
        List<String> preferredSkills,
        List<String> keywords,
        Integer experienceYears
) {}
