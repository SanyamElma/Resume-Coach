package com.resumeanalyzer.ai.cache;

import java.util.Optional;

/**
 * Cache for expensive AI outputs (LLM generations) keyed by a content hash of the full request
 * — prompt version + grounded facts + retrieved context. Caching at this layer means an identical
 * analysis/interview request costs nothing and returns instantly.
 *
 * <p>The default implementation is in-process and TTL-bounded ({@link InMemoryAiResponseCache}).
 * A Redis-backed implementation can be supplied for multi-instance deployments — it only needs to
 * implement this interface and be marked {@code @Primary} (or activated by config), since the
 * default is {@code @ConditionalOnMissingBean}.</p>
 */
public interface AiResponseCache {

    Optional<String> get(String key);

    void put(String key, String value);

    /** Atomic helper: return the cached value, or compute, store, and return it. */
    String getOrCompute(String key, java.util.function.Supplier<String> loader);
}
