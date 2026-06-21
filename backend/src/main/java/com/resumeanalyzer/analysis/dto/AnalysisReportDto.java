package com.resumeanalyzer.analysis.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full skill-gap report. The list-valued fields and the sub-scores power the analysis
 * result page and the ATS score dashboard charts on the frontend.
 */
public record AnalysisReportDto(
        UUID id,
        UUID resumeId,
        String resumeName,
        UUID jobDescriptionId,
        String jobTitle,
        int matchPercentage,
        Integer skillMatchScore,
        Integer experienceMatchScore,
        Integer educationMatchScore,
        Integer keywordMatchScore,
        List<String> missingSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        Instant createdAt
) {}
