package com.resumeanalyzer.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Structured error detail embedded in {@link ApiResponse} for failed requests.
 *
 * @param code        machine-readable error code (e.g. {@code RESOURCE_NOT_FOUND})
 * @param message     human-readable summary
 * @param path        request path that produced the error
 * @param fieldErrors per-field validation messages, when applicable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path, null);
    }
}
