package com.resumeanalyzer.analysis.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request to analyse a resume against a job description. Both must belong to the caller. */
public record RunAnalysisRequest(
        @NotNull UUID resumeId,
        @NotNull UUID jobDescriptionId
) {}
