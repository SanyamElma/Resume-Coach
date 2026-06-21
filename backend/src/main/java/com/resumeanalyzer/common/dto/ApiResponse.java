package com.resumeanalyzer.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Uniform response envelope returned by every endpoint.
 *
 * <p>A consistent shape ({@code success}, {@code message}, {@code data}, {@code error})
 * lets the frontend handle responses generically and keeps API contracts predictable.</p>
 *
 * @param <T> the type of the payload carried by successful responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorResponse error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, error.message(), null, error, Instant.now());
    }
}
