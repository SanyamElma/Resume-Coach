package com.resumeanalyzer.ai.experience;

import com.resumeanalyzer.ai.skill.SkillDictionary;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies deterministic experience math: range parsing, interval union, per-skill years. */
class ExperienceCalculatorTest {

    private final ExperienceCalculator calculator =
            new ExperienceCalculator(new SkillExtractor(new SkillDictionary()));

    @Test
    void calculate_singleRange_attributesYearsToSkills() {
        String resume = "Software Engineer, Acme. Jan 2019 - Dec 2021. Built microservices with Java and Spring Boot.";
        ExperienceResult result = calculator.calculate(resume);

        assertThat(result.totalYears()).isEqualTo(3.0); // 36 inclusive months
        assertThat(result.perSkillYears()).containsKeys("Java", "Spring Boot");
        assertThat(result.perSkillYears().get("Java")).isEqualTo(3.0);
    }

    @Test
    void calculate_overlappingRanges_areUnioned_notDoubleCounted() {
        String resume = "Role A 2019 - 2021 using Java. Role B 2020 - 2022 using React.";
        ExperienceResult result = calculator.calculate(resume);

        // Union of 2019-01..2021-12 and 2020-01..2022-12 = 2019-01..2022-12 = 48 months = 4.0
        assertThat(result.totalYears()).isEqualTo(4.0);
    }

    @Test
    void calculate_emptyText_returnsZero() {
        ExperienceResult result = calculator.calculate("");
        assertThat(result.totalYears()).isEqualTo(0.0);
        assertThat(result.perSkillYears()).isEmpty();
    }
}
