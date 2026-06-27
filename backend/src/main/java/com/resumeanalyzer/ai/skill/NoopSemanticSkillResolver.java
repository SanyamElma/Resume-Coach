package com.resumeanalyzer.ai.skill;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Set;

/**
 * Default {@link SemanticSkillResolver} used until the embedding-backed resolver (Phase 2)
 * is wired in. Returns no semantic matches, so the matcher relies purely on exact + alias
 * matching — fully deterministic and offline.
 */
@Configuration
public class NoopSemanticSkillResolver {

    @Bean
    @ConditionalOnMissingBean(SemanticSkillResolver.class)
    public SemanticSkillResolver defaultSemanticSkillResolver() {
        return (targetSkill, resumeSkills, threshold) -> Optional.empty();
    }
}
