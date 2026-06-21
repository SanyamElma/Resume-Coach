package com.resumeanalyzer.job.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record JobDescriptionDto(
        UUID id,
        String title,
        String company,
        String description,
        JsonNode structuredData,
        Instant createdAt
) {}
