package com.resumeanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an operation conflicts with current state (e.g. duplicate email on register). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
