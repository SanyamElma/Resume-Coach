package com.resumeanalyzer.interview.dto;

import com.resumeanalyzer.interview.domain.MessageSender;

import java.time.Instant;
import java.util.UUID;

public record InterviewMessageDto(
        UUID id,
        MessageSender sender,
        String message,
        Integer answerScore,
        Instant createdAt
) {}
