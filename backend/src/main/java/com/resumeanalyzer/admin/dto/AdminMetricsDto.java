package com.resumeanalyzer.admin.dto;

/** Aggregate platform metrics for the admin dashboard. */
public record AdminMetricsDto(
        long totalUsers,
        long totalResumes,
        long totalInterviews,
        long totalAnalyses,
        long newUsersLast7Days,
        long interviewsLast24Hours
) {}
