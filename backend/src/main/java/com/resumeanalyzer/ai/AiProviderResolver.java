package com.resumeanalyzer.ai;

import com.resumeanalyzer.config.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the active {@link AiProvider} implementation based on {@code app.ai.provider}.
 *
 * <p>Spring injects every {@link AiProvider} bean keyed by its bean name ({@code mock},
 * {@code openai}, {@code gemini}). This resolver is the single seam through which the rest
 * of the application obtains a provider, so changing vendors is a configuration change with
 * zero impact on calling code — the essence of the Strategy pattern.</p>
 */
@Slf4j
@Component
public class AiProviderResolver {

    private final Map<String, AiProvider> providers;
    private final String configuredProvider;

    public AiProviderResolver(Map<String, AiProvider> providers, AppProperties properties) {
        this.providers = providers;
        this.configuredProvider = properties.ai().provider().toLowerCase();
    }

    @PostConstruct
    void validate() {
        if (!providers.containsKey(configuredProvider)) {
            throw new IllegalStateException("Configured AI provider '%s' not found. Available: %s"
                    .formatted(configuredProvider, providers.keySet()));
        }
        log.info("Active AI provider: '{}' (available: {})", configuredProvider, providers.keySet());
    }

    /** Returns the provider selected by configuration. */
    public AiProvider current() {
        return providers.get(configuredProvider);
    }

    /** Returns a specific provider by name, e.g. to A/B test or override per-request. */
    public AiProvider byName(String name) {
        AiProvider provider = providers.get(name.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown AI provider: " + name);
        }
        return provider;
    }
}
