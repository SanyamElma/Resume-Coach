package com.resumeanalyzer.ai.scoring;

import com.resumeanalyzer.ai.section.ResumeSections;

/**
 * Pre-computed deterministic inputs to the {@link AtsScorer}. The skill/experience/keyword
 * sub-scores come from their dedicated engines; the resume-intrinsic dimensions
 * (education, projects, etc.) are derived by the scorer from the sections and text.
 */
public record AtsSignals(
        int skillMatchScore,
        int experienceMatchScore,
        int keywordMatchScore,
        ResumeSections sections,
        String resumeText
) {}
