package com.resumeanalyzer.ai.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/**
 * Per-model pricing for cost tracking ({@code app.ai.pricing.*}). Rates are USD per 1K tokens.
 * Values can be overridden in config; sensible public defaults are baked in so cost reporting is
 * meaningful out of the box. The {@code mock} provider is free, so it always reports zero cost.
 */
@ConfigurationProperties(prefix = "app.ai.pricing")
public record AiPricingProperties(Map<String, ModelRate> models) {

    /** USD per 1,000 tokens for each leg of a model's usage. */
    public record ModelRate(double inputPer1k, double outputPer1k, double embeddingPer1k) {
        public static final ModelRate FREE = new ModelRate(0, 0, 0);
    }

    /** Built-in defaults (USD/1K tokens) used when a model is not explicitly priced in config. */
    private static final Map<String, ModelRate> DEFAULTS = Map.of(
            "gpt-4o-mini", new ModelRate(0.00015, 0.0006, 0),
            "gpt-4o", new ModelRate(0.0025, 0.01, 0),
            "text-embedding-3-small", new ModelRate(0, 0, 0.00002),
            "text-embedding-3-large", new ModelRate(0, 0, 0.00013),
            "gemini-1.5-flash", new ModelRate(0.000075, 0.0003, 0),
            "text-embedding-004", new ModelRate(0, 0, 0.00001));

    public AiPricingProperties {
        models = models == null ? new HashMap<>() : models;
    }

    /** Resolves the rate for a model: config override → built-in default → free. */
    public ModelRate rateFor(String model) {
        if (model == null) {
            return ModelRate.FREE;
        }
        String key = model.toLowerCase(Locale.ROOT);
        ModelRate configured = models.get(model);
        if (configured == null) {
            configured = models.get(key);
        }
        if (configured != null) {
            return configured;
        }
        return DEFAULTS.getOrDefault(key, ModelRate.FREE);
    }

    public double inputCost(String model, int tokens) {
        return rateFor(model).inputPer1k() * tokens / 1000.0;
    }

    public double outputCost(String model, int tokens) {
        return rateFor(model).outputPer1k() * tokens / 1000.0;
    }

    public double embeddingCost(String model, int tokens) {
        return rateFor(model).embeddingPer1k() * tokens / 1000.0;
    }
}
