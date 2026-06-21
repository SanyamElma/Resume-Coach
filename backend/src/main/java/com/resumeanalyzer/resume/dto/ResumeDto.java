package com.resumeanalyzer.resume.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/** Full resume representation including parsed content (for the detail view). */
public record ResumeDto(
        UUID id,
        String resumeName,
        String contentType,
        Long sizeBytes,
        int version,
        String parsedText,
        JsonNode parsedData,
        Instant createdAt
) {}
