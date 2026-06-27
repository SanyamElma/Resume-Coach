package com.resumeanalyzer.ai.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies alias resolution, word-boundary extraction, and weighted matching — all offline. */
class SkillEngineTest {

    private final SkillDictionary dictionary = new SkillDictionary();
    private final SkillExtractor extractor = new SkillExtractor(dictionary);
    // No-op semantic layer for Phase 1 (embedding-backed resolver arrives in Phase 2).
    private final SkillMatcher matcher = new SkillMatcher(
            (target, resume, threshold) -> Optional.empty());

    @Test
    void extract_resolvesAliasesToCanonicalSkills() {
        Set<String> skills = extractor.extract("Proficient in JS, ReactJS, Node and k8s.");
        assertThat(skills).contains("JavaScript", "React", "Node.js", "Kubernetes");
    }

    @Test
    void extract_respectsWordBoundaries_noFalsePositives() {
        // "Go" must not match inside "Google"; "JS" must not match inside "jstor".
        Set<String> skills = extractor.extract("I used Google products and read jstor articles.");
        assertThat(skills).doesNotContain("Go", "JavaScript");
    }

    @Test
    void match_combinesRequiredAndPreferredWithWeighting() {
        SkillMatchResult result = matcher.match(
                Set.of("Java", "React"),
                List.of("Java", "Spring Boot"),   // required
                List.of("React"));                // preferred

        assertThat(result.matchedRequired()).containsExactly("Java");
        assertThat(result.missingRequired()).containsExactly("Spring Boot");
        assertThat(result.matchedPreferred()).containsExactly("React");
        // 0.8 * (1/2) + 0.2 * (1/1) = 0.6
        assertThat(result.score()).isEqualTo(60);
    }

    @Test
    void dictionary_normalizesKnownAlias() {
        Optional<SkillDictionary.Skill> skill = dictionary.normalize("springboot");
        assertThat(skill).isPresent();
        assertThat(skill.get().canonical()).isEqualTo("Spring Boot");
        assertThat(skill.get().category()).isEqualTo(SkillDictionary.Category.FRAMEWORK);
    }
}
