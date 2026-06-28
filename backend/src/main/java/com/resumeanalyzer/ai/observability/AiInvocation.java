package com.resumeanalyzer.ai.observability;

/**
 * One recorded AI invocation — the unit of observability. Captures which operation ran, which
 * versioned prompt and model produced it, how long it took, token usage and estimated cost,
 * whether it was served from cache, and how much grounding context was retrieved.
 *
 * <p>Use the {@link Builder} so call sites read clearly and unset fields default sanely.</p>
 */
public record AiInvocation(
        String operation,     // e.g. "analysis.explain", "interview.questions"
        String promptId,      // versioned prompt id, e.g. "analysis.explain@v1" (nullable)
        String provider,      // mock | openai | gemini
        String model,         // concrete model name (nullable)
        long latencyMs,
        int inputTokens,
        int outputTokens,
        int embeddingTokens,
        double costUsd,
        boolean cacheHit,
        int retrievedChunks,
        double topSimilarity,
        boolean fallback      // true when the LLM path failed/was skipped and a deterministic result was used
) {
    public static Builder builder(String operation) {
        return new Builder(operation);
    }

    public static final class Builder {
        private final String operation;
        private String promptId;
        private String provider = "mock";
        private String model;
        private long latencyMs;
        private int inputTokens;
        private int outputTokens;
        private int embeddingTokens;
        private double costUsd;
        private boolean cacheHit;
        private int retrievedChunks;
        private double topSimilarity;
        private boolean fallback;

        private Builder(String operation) {
            this.operation = operation;
        }

        public Builder promptId(String v) { this.promptId = v; return this; }
        public Builder provider(String v) { this.provider = v; return this; }
        public Builder model(String v) { this.model = v; return this; }
        public Builder latencyMs(long v) { this.latencyMs = v; return this; }
        public Builder inputTokens(int v) { this.inputTokens = v; return this; }
        public Builder outputTokens(int v) { this.outputTokens = v; return this; }
        public Builder embeddingTokens(int v) { this.embeddingTokens = v; return this; }
        public Builder costUsd(double v) { this.costUsd = v; return this; }
        public Builder cacheHit(boolean v) { this.cacheHit = v; return this; }
        public Builder retrievedChunks(int v) { this.retrievedChunks = v; return this; }
        public Builder topSimilarity(double v) { this.topSimilarity = v; return this; }
        public Builder fallback(boolean v) { this.fallback = v; return this; }

        public AiInvocation build() {
            return new AiInvocation(operation, promptId, provider, model, latencyMs,
                    inputTokens, outputTokens, embeddingTokens, costUsd, cacheHit,
                    retrievedChunks, topSimilarity, fallback);
        }
    }
}
