package com.resumeanalyzer.ai.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Central sink for AI observability. Every LLM/embedding-bearing operation reports an
 * {@link AiInvocation}; this service emits a structured log line (one per call, parseable by any
 * log aggregator) and maintains in-process aggregates (token totals, cost, latency, cache-hit
 * rate, per-operation counts) surfaced to the admin dashboard via {@link #snapshot()}.
 *
 * <p>All counters are lock-free and thread-safe, so recording adds negligible overhead to the
 * request path.</p>
 */
@Slf4j
@Service
public class AiTelemetry {

    private final LongAdder invocations = new LongAdder();
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder fallbacks = new LongAdder();
    private final LongAdder inputTokens = new LongAdder();
    private final LongAdder outputTokens = new LongAdder();
    private final LongAdder embeddingTokens = new LongAdder();
    private final DoubleAdder costUsd = new DoubleAdder();
    private final DoubleAdder latencyMs = new DoubleAdder();
    private final Map<String, AtomicLong> byOperation = new ConcurrentHashMap<>();

    public void record(AiInvocation inv) {
        invocations.increment();
        if (inv.cacheHit()) {
            cacheHits.increment();
        }
        if (inv.fallback()) {
            fallbacks.increment();
        }
        inputTokens.add(inv.inputTokens());
        outputTokens.add(inv.outputTokens());
        embeddingTokens.add(inv.embeddingTokens());
        costUsd.add(inv.costUsd());
        latencyMs.add(inv.latencyMs());
        byOperation.computeIfAbsent(inv.operation(), k -> new AtomicLong()).incrementAndGet();

        // Structured, single-line event for log-based observability.
        log.info("ai_invocation op={} prompt={} provider={} model={} latencyMs={} "
                        + "inTok={} outTok={} embTok={} costUsd={} cacheHit={} chunks={} topSim={} fallback={}",
                inv.operation(), inv.promptId(), inv.provider(), inv.model(), inv.latencyMs(),
                inv.inputTokens(), inv.outputTokens(), inv.embeddingTokens(),
                String.format("%.6f", inv.costUsd()), inv.cacheHit(),
                inv.retrievedChunks(), String.format("%.3f", inv.topSimilarity()), inv.fallback());
    }

    public AiUsageSnapshot snapshot() {
        long total = invocations.sum();
        long hits = cacheHits.sum();
        double hitRate = total == 0 ? 0.0 : (double) hits / total;
        double avgLatency = total == 0 ? 0.0 : latencyMs.sum() / total;

        Map<String, Long> opCounts = new ConcurrentHashMap<>();
        byOperation.forEach((k, v) -> opCounts.put(k, v.get()));

        return new AiUsageSnapshot(
                total, hits, hitRate, fallbacks.sum(),
                inputTokens.sum(), outputTokens.sum(), embeddingTokens.sum(),
                costUsd.sum(), avgLatency, opCounts);
    }
}
