package com.resumeanalyzer.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.model.JobAnalysis;
import com.resumeanalyzer.ai.support.JsonExtractor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ensures the JSON extractor tolerates the markdown/prose wrappers LLMs commonly emit. */
class JsonExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parse_stripsMarkdownCodeFences() {
        String raw = """
                Here is the analysis:
                ```json
                { "requiredSkills": ["java"], "preferredSkills": [], "keywords": ["spring"], "experienceYears": 3 }
                ```
                """;
        JobAnalysis result = JsonExtractor.parse(mapper, raw, JobAnalysis.class);

        assertThat(result.requiredSkills()).containsExactly("java");
        assertThat(result.experienceYears()).isEqualTo(3);
    }

    @Test
    void parse_handlesPlainJson() {
        String raw = "{ \"requiredSkills\": [], \"preferredSkills\": [], \"keywords\": [], \"experienceYears\": 0 }";
        JobAnalysis result = JsonExtractor.parse(mapper, raw, JobAnalysis.class);
        assertThat(result.keywords()).isEmpty();
    }
}
