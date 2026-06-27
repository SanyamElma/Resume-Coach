package com.resumeanalyzer.ai.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministically matches resume skills against job requirements by combining techniques
 * in priority order: EXACT/ALIAS (via canonicalisation in {@link SkillExtractor}) first,
 * then a pluggable SEMANTIC layer for the remainder.
 *
 * <p>The produced score is a transparent, reproducible weighted coverage — the LLM is later
 * asked only to <em>explain</em> it, never to compute it.</p>
 */
@Component
@RequiredArgsConstructor
public class SkillMatcher {

    private static final double SEMANTIC_THRESHOLD = 0.82;
    private static final double REQUIRED_WEIGHT = 0.8;
    private static final double PREFERRED_WEIGHT = 0.2;

    private final SemanticSkillResolver semanticResolver;

    /**
     * @param resumeSkills    canonical skills extracted from the resume
     * @param requiredSkills  canonical required skills from the JD
     * @param preferredSkills canonical preferred skills from the JD
     */
    public SkillMatchResult match(Set<String> resumeSkills,
                                  List<String> requiredSkills,
                                  List<String> preferredSkills) {
        Set<String> resume = new LinkedHashSet<>(resumeSkills);

        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        List<String> matchedPreferred = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();
        List<SkillMatchResult.SkillMatch> matches = new ArrayList<>();

        for (String req : dedupe(requiredSkills)) {
            classify(req, resume, true, matchedRequired, missingRequired, matches);
        }
        for (String pref : dedupe(preferredSkills)) {
            if (matchedRequired.contains(pref) || missingRequired.contains(pref)) {
                continue; // already accounted for as a required skill
            }
            classify(pref, resume, false, matchedPreferred, missingPreferred, matches);
        }

        int score = computeScore(matchedRequired.size(), missingRequired.size(),
                matchedPreferred.size(), missingPreferred.size());

        return new SkillMatchResult(score, matchedRequired, missingRequired,
                matchedPreferred, missingPreferred, matches);
    }

    private void classify(String skill, Set<String> resume, boolean required,
                          List<String> matched, List<String> missing,
                          List<SkillMatchResult.SkillMatch> matches) {
        if (resume.contains(skill)) {
            matched.add(skill);
            matches.add(new SkillMatchResult.SkillMatch(skill, SkillMatchResult.Technique.EXACT, 1.0, required));
            return;
        }
        var semantic = semanticResolver.resolveSimilar(skill, resume, SEMANTIC_THRESHOLD);
        if (semantic.isPresent()) {
            matched.add(skill);
            matches.add(new SkillMatchResult.SkillMatch(skill, SkillMatchResult.Technique.SEMANTIC,
                    SEMANTIC_THRESHOLD, required));
            return;
        }
        missing.add(skill);
        matches.add(new SkillMatchResult.SkillMatch(skill, SkillMatchResult.Technique.NONE, 0.0, required));
    }

    private int computeScore(int matchedReq, int missingReq, int matchedPref, int missingPref) {
        int totalReq = matchedReq + missingReq;
        int totalPref = matchedPref + missingPref;

        if (totalReq == 0 && totalPref == 0) {
            return 0; // nothing to match against
        }
        double reqCoverage = totalReq == 0 ? 1.0 : (double) matchedReq / totalReq;
        double prefCoverage = totalPref == 0 ? 0.0 : (double) matchedPref / totalPref;

        double weighted;
        if (totalPref == 0) {
            weighted = reqCoverage;
        } else if (totalReq == 0) {
            weighted = prefCoverage;
        } else {
            weighted = REQUIRED_WEIGHT * reqCoverage + PREFERRED_WEIGHT * prefCoverage;
        }
        return (int) Math.round(weighted * 100);
    }

    private List<String> dedupe(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(values));
    }
}
