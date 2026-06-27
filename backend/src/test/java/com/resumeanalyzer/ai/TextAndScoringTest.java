package com.resumeanalyzer.ai;

import com.resumeanalyzer.ai.jd.JobRequirementExtractor;
import com.resumeanalyzer.ai.jd.JobRequirements;
import com.resumeanalyzer.ai.scoring.AtsBreakdown;
import com.resumeanalyzer.ai.scoring.AtsScorer;
import com.resumeanalyzer.ai.scoring.AtsSignals;
import com.resumeanalyzer.ai.section.ResumeSectionDetector;
import com.resumeanalyzer.ai.section.ResumeSections;
import com.resumeanalyzer.ai.section.ResumeSections.SectionType;
import com.resumeanalyzer.ai.skill.SkillDictionary;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import com.resumeanalyzer.ai.text.ResumeCleaner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers cleaning, section detection, JD parsing, and weighted ATS scoring. */
class TextAndScoringTest {

    private final ResumeCleaner cleaner = new ResumeCleaner();
    private final ResumeSectionDetector sectionDetector = new ResumeSectionDetector();
    private final AtsScorer atsScorer = new AtsScorer();
    private final JobRequirementExtractor jdExtractor =
            new JobRequirementExtractor(new SkillExtractor(new SkillDictionary()));

    @Test
    void cleaner_removesHiddenChars_pageMarkers_andRepeatedHeaders() {
        String raw = "ACME HEADER\nJohn Doe​\nPage 1 of 3\nReal content line\n"
                + "ACME HEADER\nMore content\nACME HEADER\nEnd\nACME HEADER";
        String cleaned = cleaner.clean(raw);

        assertThat(cleaned).doesNotContain("ACME HEADER");   // repeated 4x → header/footer
        assertThat(cleaned).doesNotContain("Page 1 of 3");
        assertThat(cleaned).doesNotContain("​");
        assertThat(cleaned).contains("Real content line");
    }

    @Test
    void sectionDetector_splitsKnownSections() {
        String resume = "John Doe\nSummary\nExperienced backend engineer.\n"
                + "Skills\nJava, React, Docker\nExperience\nAcme - Software Engineer";
        ResumeSections sections = sectionDetector.detect(resume);

        assertThat(sections.has(SectionType.SUMMARY)).isTrue();
        assertThat(sections.get(SectionType.SKILLS)).contains("React");
        assertThat(sections.has(SectionType.EXPERIENCE)).isTrue();
    }

    @Test
    void jdExtractor_separatesRequiredFromPreferred_andReadsYearsSeniority() {
        String jd = "Senior Backend Engineer. 3+ years experience with Java and Spring Boot required. "
                + "Nice to have: Kubernetes and AWS.";
        JobRequirements req = jdExtractor.extract(jd);

        assertThat(req.requiredSkills()).contains("Java", "Spring Boot");
        assertThat(req.preferredSkills()).contains("Kubernetes", "AWS");
        assertThat(req.requiredSkills()).doesNotContain("Kubernetes");
        assertThat(req.minYearsExperience()).isEqualTo(3);
        assertThat(req.seniority()).isEqualTo(JobRequirements.Seniority.SENIOR);
    }

    @Test
    void atsScorer_producesWeightedDeterministicBreakdown() {
        ResumeSections sections = new ResumeSections(Map.of(
                SectionType.EDUCATION, "Bachelor of Technology in Computer Science",
                SectionType.SKILLS, "Java, Spring Boot",
                SectionType.PROJECTS, "Project A\nProject B",
                SectionType.EXPERIENCE, "Acme Software Engineer 2019-2021"));
        String text = "john@example.com +1 555 123 4567. Reduced latency by 40% across 1000 requests. "
                + "Bachelor of Technology. Built scalable services over several years of work experience here.";

        AtsBreakdown b = atsScorer.score(new AtsSignals(90, 80, 70, sections, text));

        assertThat(b.education()).isEqualTo(88);   // bachelor (not master)
        assertThat(b.certifications()).isEqualTo(55); // no cert section
        assertThat(b.overall()).isBetween(0, 100);
        assertThat(b.weights().values().stream().mapToDouble(Double::doubleValue).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }
}
