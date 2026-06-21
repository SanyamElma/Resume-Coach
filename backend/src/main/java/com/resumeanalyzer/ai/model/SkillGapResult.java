package com.resumeanalyzer.ai.model;

import java.util.List;

/**
 * Output of comparing a resume against a job description. Matches the JSON contract
 * documented in the spec: {@code matchScore, missingSkills, strengths, weaknesses,
 * recommendations} — extended with ATS sub-scores for the dashboard.
 */
public record SkillGapResult(
        int matchScore,
        int skillMatchScore,
        int experienceMatchScore,
        int educationMatchScore,
        int keywordMatchScore,
        List<String> missingSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations
) {}
