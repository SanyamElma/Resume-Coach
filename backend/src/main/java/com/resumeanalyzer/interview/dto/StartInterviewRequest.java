package com.resumeanalyzer.interview.dto;

import java.util.UUID;

/** Request to begin a mock interview, optionally anchored to a resume and target role. */
public record StartInterviewRequest(
        UUID resumeId,
        UUID jobDescriptionId
) {}
