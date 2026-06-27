package com.resumeanalyzer.ai.scoring;

import java.util.Map;

/**
 * Transparent, reproducible ATS score with its per-dimension components and the weights
 * used. The LLM later explains this breakdown — it never produces the numbers.
 */
public record AtsBreakdown(
        int overall,
        int skills,
        int experience,
        int keywords,
        int projects,
        int education,
        int certifications,
        int achievements,
        int formatting,
        Map<String, Double> weights
) {}
