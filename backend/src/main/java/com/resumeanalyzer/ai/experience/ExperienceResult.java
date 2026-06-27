package com.resumeanalyzer.ai.experience;

import java.util.Map;

/**
 * Deterministically computed experience figures.
 *
 * @param totalYears   total professional experience (union of all date ranges), 1 decimal
 * @param perSkillYears years of hands-on experience per canonical skill, derived from the
 *                      date ranges of the roles/blocks in which each skill appears
 */
public record ExperienceResult(
        double totalYears,
        Map<String, Double> perSkillYears
) {}
