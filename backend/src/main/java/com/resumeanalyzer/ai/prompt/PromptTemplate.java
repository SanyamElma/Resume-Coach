package com.resumeanalyzer.ai.prompt;

import java.util.Map;

/**
 * A versioned, reusable prompt template. The {@code system} text sets role + rules + output
 * schema; the {@code userTemplate} carries {@code {{placeholder}}} slots filled at build time
 * with deterministic facts and retrieved context. Versioning makes prompt changes auditable
 * and lets us log exactly which prompt produced a given output.
 */
public record PromptTemplate(String id, String version, String system, String userTemplate) {

    /** Renders the user template by substituting {@code {{key}}} placeholders. */
    public String renderUser(Map<String, String> variables) {
        String result = userTemplate;
        for (var entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public String fullId() {
        return id + "@" + version;
    }
}
