package com.resumeanalyzer.job.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.job.dto.JobDescriptionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobMapper {

    private final ObjectMapper objectMapper;

    public JobDescriptionDto toDto(JobDescription jd) {
        return new JobDescriptionDto(jd.getId(), jd.getTitle(), jd.getCompany(), jd.getDescription(),
                readJson(jd.getStructuredData()), jd.getCreatedAt());
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse stored JD JSON: {}", e.getMessage());
            return null;
        }
    }
}
