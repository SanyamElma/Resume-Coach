package com.resumeanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist or is not visible to the caller. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException("%s not found with id: %s".formatted(entity, id));
    }
}
