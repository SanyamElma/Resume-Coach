package com.resumeanalyzer.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for all application-level exceptions. Carries an HTTP status and a stable
 * machine-readable error code so the {@code GlobalExceptionHandler} can translate
 * domain failures into consistent HTTP responses.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
