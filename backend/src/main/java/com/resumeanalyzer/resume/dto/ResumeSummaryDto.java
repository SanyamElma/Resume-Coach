package com.resumeanalyzer.resume.dto;

import java.time.Instant;
import java.util.UUID;

/** Lightweight resume representation for list/history views (no heavy parsed text). */
public record ResumeSummaryDto(
        UUID id,
        String resumeName,
        String contentType,
        Long sizeBytes,
        int version,
        Instant createdAt
) {}
