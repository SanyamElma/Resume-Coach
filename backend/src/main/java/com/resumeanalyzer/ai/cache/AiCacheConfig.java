package com.resumeanalyzer.ai.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the default {@link AiResponseCache}. Using a {@code @Bean} method with
 * {@link ConditionalOnMissingBean} (rather than the annotation on the component class) makes the
 * Redis-style override reliable: dropping any other {@link AiResponseCache} bean on the classpath
 * transparently replaces the in-memory default.
 */
@Configuration
public class AiCacheConfig {

    @Bean
    @ConditionalOnMissingBean(AiResponseCache.class)
    public AiResponseCache inMemoryAiResponseCache(
            @Value("${app.ai.cache.ttl-seconds:3600}") long ttlSeconds,
            @Value("${app.ai.cache.max-entries:1000}") int maxEntries) {
        return new InMemoryAiResponseCache(ttlSeconds, maxEntries);
    }
}
