package com.resumeanalyzer.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.common.exception.AiProviderException;

/**
 * Parses JSON out of LLM responses. Models occasionally wrap JSON in markdown code fences
 * or add prose; this helper strips fences and isolates the first balanced JSON object/array
 * before delegating to Jackson.
 */
public final class JsonExtractor {

    private JsonExtractor() {
    }

    public static <T> T parse(ObjectMapper mapper, String raw, Class<T> type) {
        try {
            return mapper.readValue(isolate(raw), type);
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse AI response as " + type.getSimpleName(), e);
        }
    }

    public static <T> T parse(ObjectMapper mapper, String raw,
                              com.fasterxml.jackson.core.type.TypeReference<T> type) {
        try {
            return mapper.readValue(isolate(raw), type);
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse AI response", e);
        }
    }

    /** Strips markdown fences and returns the substring spanning the outermost JSON braces. */
    static String isolate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiProviderException("Empty AI response");
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)^```(?:json)?", "").replaceAll("```\\s*$", "").trim();
        }
        int objStart = cleaned.indexOf('{');
        int arrStart = cleaned.indexOf('[');
        int start = (arrStart != -1 && (objStart == -1 || arrStart < objStart)) ? arrStart : objStart;
        char open = start == arrStart ? '[' : '{';
        char close = open == '[' ? ']' : '}';
        int end = cleaned.lastIndexOf(close);
        if (start == -1 || end == -1 || end < start) {
            return cleaned; // let Jackson surface a precise parse error
        }
        return cleaned.substring(start, end + 1);
    }
}
