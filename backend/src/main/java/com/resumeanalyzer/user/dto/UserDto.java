package com.resumeanalyzer.user.dto;

import com.resumeanalyzer.user.domain.Role;

import java.time.Instant;
import java.util.UUID;

/** Public representation of a user. Never exposes the password hash. */
public record UserDto(
        UUID id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {}
