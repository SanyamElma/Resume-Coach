package com.resumeanalyzer.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Helpers for persisting/reading {@code List<String>} as JSON-encoded text columns —
 * portable across databases without relying on vendor-specific array types.
 */
public final class JsonLists {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private JsonLists() {
    }

    public static String toJson(ObjectMapper mapper, List<String> values) {
        try {
            return mapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<String> fromJson(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }
}
