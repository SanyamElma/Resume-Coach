package com.resumeanalyzer.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload to create a job description. The platform structures it via the AI provider. */
public record CreateJobRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String company,
        @NotBlank @Size(max = 20000) String description
) {}
