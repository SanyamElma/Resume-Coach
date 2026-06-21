package com.resumeanalyzer.user.domain;

/**
 * Application roles. Stored as strings in the database for readability and forward
 * compatibility. Spring Security authorities are derived as {@code ROLE_<name>}.
 */
public enum Role {
    CANDIDATE,
    ADMIN
}
