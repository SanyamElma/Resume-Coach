package com.resumeanalyzer.ai.observability;

import java.util.Map;

/** Immutable aggregate of AI usage since startup, surfaced to the admin dashboard. */
public record AiUsageSnapshot(
        long totalInvocations,
        long cacheHits,
        double cacheHitRate,
        long llmFallbacks,
        long totalInputTokens,
        long totalOutputTokens,
        long totalEmbeddingTokens,
        double totalCostUsd,
        double avgLatencyMs,
        Map<String, Long> invocationsByOperation
) {}
