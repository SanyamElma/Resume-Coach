package com.resumeanalyzer.ai.skill;

import java.util.List;

/**
 * Outcome of matching a resume's skills against a job's required/preferred skills.
 *
 * @param score            0-100 weighted coverage (required weighted higher than preferred)
 * @param matchedRequired  required skills found in the resume
 * @param missingRequired  required skills absent from the resume
 * @param matchedPreferred preferred skills found in the resume
 * @param missingPreferred preferred skills absent from the resume
 * @param matches          per-skill match detail (technique used, confidence)
 */
public record SkillMatchResult(
        int score,
        List<String> matchedRequired,
        List<String> missingRequired,
        List<String> matchedPreferred,
        List<String> missingPreferred,
        List<SkillMatch> matches
) {
    /** Per-skill provenance: which technique matched it and at what confidence. */
    public record SkillMatch(String skill, Technique technique, double confidence, boolean required) {}

    public enum Technique { EXACT, ALIAS, SEMANTIC, NONE }
}
