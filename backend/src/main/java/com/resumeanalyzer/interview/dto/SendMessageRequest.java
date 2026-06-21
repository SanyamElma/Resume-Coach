package com.resumeanalyzer.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** A candidate's answer within an ongoing interview session. */
public record SendMessageRequest(
        @NotNull UUID sessionId,
        @NotBlank @Size(max = 5000) String message
) {}
