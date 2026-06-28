package com.resumeanalyzer.ai.security;

import java.util.List;

/**
 * Outcome of sanitizing untrusted text before it is embedded into a prompt.
 *
 * @param sanitized the cleaned text, safe to interpolate into a prompt template
 * @param flagged   human-readable descriptions of injection patterns that were neutralized
 */
public record SanitizationResult(String sanitized, List<String> flagged) {
    public boolean wasModified() {
        return !flagged.isEmpty();
    }
}
