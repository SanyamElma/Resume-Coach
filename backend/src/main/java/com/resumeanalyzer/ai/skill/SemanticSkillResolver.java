package com.resumeanalyzer.ai.skill;

import java.util.Optional;
import java.util.Set;

/**
 * Pluggable semantic matching layer for skills that are not caught by exact/alias matching
 * (e.g. close synonyms or related technologies not yet in the dictionary).
 *
 * <p>Phase 1 ships a {@link NoopSemanticSkillResolver}; Phase 2 provides an embedding-backed
 * implementation that compares vector cosine similarity above a threshold. Keeping this as a
 * seam means the matcher's combination logic is written once and never changes.</p>
 */
public interface SemanticSkillResolver {

    /**
     * Attempts to find a resume skill semantically equivalent to the target skill.
     *
     * @return the matching resume skill if confidence ≥ threshold, else empty
     */
    Optional<String> resolveSimilar(String targetSkill, Set<String> resumeSkills, double threshold);
}
