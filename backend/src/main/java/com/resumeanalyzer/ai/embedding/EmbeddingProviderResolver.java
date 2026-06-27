package com.resumeanalyzer.ai.embedding;

import com.resumeanalyzer.ai.config.AiEngineProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selects the active {@link EmbeddingProvider} by {@code app.ai.engine.embedding-provider}.
 * Providers are indexed by their {@link EmbeddingProvider#name()} (not Spring bean name) to
 * avoid collisions with the LLM provider beans.
 */
@Slf4j
@Component
public class EmbeddingProviderResolver {

    private final Map<String, EmbeddingProvider> providers;
    private final String active;

    public EmbeddingProviderResolver(List<EmbeddingProvider> providerBeans, AiEngineProperties properties) {
        this.providers = providerBeans.stream()
                .collect(Collectors.toMap(p -> p.name().toLowerCase(Locale.ROOT), Function.identity()));
        this.active = properties.embeddingProvider().toLowerCase(Locale.ROOT);
    }

    @PostConstruct
    void validate() {
        if (!providers.containsKey(active)) {
            throw new IllegalStateException("Configured embedding provider '%s' not found. Available: %s"
                    .formatted(active, providers.keySet()));
        }
        log.info("Active embedding provider: '{}' ({} dims, available: {})",
                active, current().dimensions(), providers.keySet());
    }

    public EmbeddingProvider current() {
        return providers.get(active);
    }
}
