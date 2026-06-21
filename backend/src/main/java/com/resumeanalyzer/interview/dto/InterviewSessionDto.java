package com.resumeanalyzer.interview.dto;

import com.resumeanalyzer.interview.domain.InterviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewSessionDto(
        UUID id,
        InterviewStatus status,
        Integer score,
        Integer communicationScore,
        Integer technicalScore,
        Integer confidenceScore,
        String feedback,
        Instant createdAt,
        List<InterviewMessageDto> messages
) {}
