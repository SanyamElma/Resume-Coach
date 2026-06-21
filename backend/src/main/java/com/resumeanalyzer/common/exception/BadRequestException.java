package com.resumeanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown for invalid client input that bean-validation cannot express declaratively. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
