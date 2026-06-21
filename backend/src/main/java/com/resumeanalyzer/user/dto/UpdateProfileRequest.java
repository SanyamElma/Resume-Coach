package com.resumeanalyzer.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable profile fields. Email changes are intentionally excluded for now. */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String name
) {}
