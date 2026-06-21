package com.resumeanalyzer.dashboard.dto;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated analytics for the candidate dashboard. Each series maps directly onto a
 * Recharts component on the frontend.
 */
public record DashboardDto(
        long totalResumes,
        long totalAnalyses,
        long totalInterviews,
        Integer latestMatchScore,
        List<TrendPoint> resumeScoreTrend,
        List<TrendPoint> interviewScoreTrend,
        List<FrequencyPoint> skillDistribution,
        List<FrequencyPoint> missingSkillsFrequency
) {
    /** A single time-series data point. */
    public record TrendPoint(Instant date, int score, String label) {}

    /** A single categorical frequency data point. */
    public record FrequencyPoint(String name, int count) {}
}
