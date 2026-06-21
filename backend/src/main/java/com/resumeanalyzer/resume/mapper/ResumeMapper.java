package com.resumeanalyzer.resume.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.resume.domain.Resume;
import com.resumeanalyzer.resume.dto.ResumeDto;
import com.resumeanalyzer.resume.dto.ResumeSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Maps {@link Resume} entities to DTOs, deserializing the stored parsed-data JSON. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeMapper {

    private final ObjectMapper objectMapper;

    public ResumeSummaryDto toSummary(Resume resume) {
        return new ResumeSummaryDto(resume.getId(), resume.getResumeName(), resume.getContentType(),
                resume.getSizeBytes(), resume.getVersion(), resume.getCreatedAt());
    }

    public ResumeDto toDto(Resume resume) {
        return new ResumeDto(resume.getId(), resume.getResumeName(), resume.getContentType(),
                resume.getSizeBytes(), resume.getVersion(), resume.getParsedText(),
                readJson(resume.getParsedData()), resume.getCreatedAt());
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse stored resume JSON: {}", e.getMessage());
            return null;
        }
    }
}
