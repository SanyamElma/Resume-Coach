package com.resumeanalyzer.security;

import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Static accessors for the currently authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserDetails currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails principal)) {
            throw new ResourceNotFoundException("No authenticated user in context");
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentPrincipal().getId();
    }
}
