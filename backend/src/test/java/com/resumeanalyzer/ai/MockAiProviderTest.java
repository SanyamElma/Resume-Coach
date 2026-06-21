package com.resumeanalyzer.ai;

import com.resumeanalyzer.ai.model.InterviewQuestion;
import com.resumeanalyzer.ai.model.SkillGapResult;
import com.resumeanalyzer.ai.provider.MockAiProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the deterministic heuristics of {@link MockAiProvider}. Because it is offline,
 * these are fast, pure unit tests with no Spring context or network.
 */
class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void analyzeSkillGap_scoresHigherWhenSkillsOverlap() {
        String resume = "Experienced Java developer skilled in Spring Boot, PostgreSQL and Docker.";
        String strongJd = "Looking for a Java engineer with Spring Boot and PostgreSQL experience.";
        String weakJd = "Looking for a Python data scientist with TensorFlow and Kafka.";

        SkillGapResult strong = provider.analyzeSkillGap(resume, strongJd);
        SkillGapResult weak = provider.analyzeSkillGap(resume, weakJd);

        assertThat(strong.matchScore()).isGreaterThan(weak.matchScore());
        assertThat(strong.matchScore()).isBetween(0, 100);
        assertThat(strong.missingSkills()).doesNotContain("java");
    }

    @Test
    void analyzeResume_extractsEmailAndSkills() {
        String resume = "Jane Doe\njane.doe@example.com\nSkills: Java, React, Docker";
        var parsed = provider.analyzeResume(resume);

        assertThat(parsed.email()).isEqualTo("jane.doe@example.com");
        assertThat(parsed.skills()).contains("java", "react", "docker");
    }

    @Test
    void generateQuestions_returnsRequestedCountAcrossCategories() {
        List<InterviewQuestion> questions = provider.generateQuestions(
                "Java Spring Boot developer", "Backend engineer role using Java and Kafka", 20);

        assertThat(questions).hasSize(20);
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.category()).isIn("TECHNICAL", "BEHAVIORAL", "HR", "SYSTEM_DESIGN");
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.question()).isNotBlank();
        });
    }
}
