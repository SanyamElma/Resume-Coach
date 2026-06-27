package com.resumeanalyzer.ai.embedding;

import com.resumeanalyzer.ai.config.AiEngineProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Deterministic, offline embedding provider (the default). Produces a hashed
 * bag-of-words vector: tokens are hashed into dimensions with sub-linear term weighting,
 * then L2-normalised. Texts sharing vocabulary get high cosine similarity, so the full
 * RAG pipeline (chunk → embed → vector search → retrieve) works without any API key or
 * cost, and is fully reproducible in tests/CI.
 */
@Component
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimensions;

    public MockEmbeddingProvider(AiEngineProperties properties) {
        this.dimensions = properties.embeddingDimensions();
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String model() {
        return "hashed-bow-v1";
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vec = new float[dimensions];
        if (text == null || text.isBlank()) {
            return vec;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+")) {
            if (token.length() < 2) {
                continue;
            }
            // Two hashes per token reduce collisions and approximate a richer feature space.
            int h1 = Math.floorMod(token.hashCode(), dimensions);
            int h2 = Math.floorMod((token + "#salt").hashCode(), dimensions);
            vec[h1] += 1.0f;
            vec[h2] += 0.5f;
        }
        return Vectors.normalize(vec);
    }
}
