package com.resumeanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an upstream AI provider call fails or returns an unusable response. */
public class AiProviderException extends ApiException {

    public AiProviderException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", message);
        initCause(cause);
    }
}
