package com.resumeanalyzer.ai.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default in-process {@link AiResponseCache}: an LRU map (size-bounded) with per-entry TTL
 * expiry checked lazily on read. Dependency-free and safe for the single-instance free-tier
 * deployment; a Redis bean can transparently replace it for horizontal scaling — it is wired in
 * {@link AiCacheConfig} as {@code @ConditionalOnMissingBean}, so supplying another
 * {@link AiResponseCache} bean overrides it.
 */
@Slf4j
public class InMemoryAiResponseCache implements AiResponseCache {

    private record CacheEntry(String value, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final long ttlMillis;
    private final Map<String, CacheEntry> store;

    public InMemoryAiResponseCache(long ttlSeconds, int maxEntries) {
        this.ttlMillis = ttlSeconds * 1000L;
        this.store = java.util.Collections.synchronizedMap(
                new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                        return size() > maxEntries;
                    }
                });
        log.info("AI response cache: in-memory LRU (ttl={}s, maxEntries={})", ttlSeconds, maxEntries);
    }

    @Override
    public Optional<String> get(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, String value) {
        if (value == null) {
            return;
        }
        store.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlMillis));
    }

    @Override
    public String getOrCompute(String key, Supplier<String> loader) {
        Optional<String> cached = get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String computed = loader.get();
        put(key, computed);
        return computed;
    }
}
