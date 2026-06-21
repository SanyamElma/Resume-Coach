package com.resumeanalyzer.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Strongly-typed binding for the {@code app.*} configuration namespace.
 *
 * <p>Centralising configuration in immutable records gives us validation at startup,
 * IDE autocompletion, and a single source of truth instead of scattered
 * {@code @Value} annotations.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Storage storage,
        Ai ai
) {

    public record Security(Jwt jwt, Cors cors) {
        public record Jwt(
                @NotBlank String secret,
                Duration accessTokenTtl,
                Duration refreshTokenTtl,
                String issuer
        ) {}

        public record Cors(List<String> allowedOrigins) {}
    }

    public record Storage(
            String provider,
            Local local,
            long maxFileSizeBytes
    ) {
        public record Local(String basePath) {}
    }

    public record Ai(
            String provider,
            Duration requestTimeout,
            OpenAi openai,
            Gemini gemini
    ) {
        public record OpenAi(String apiKey, String baseUrl, String model) {}

        public record Gemini(String apiKey, String baseUrl, String model) {}
    }
}
