package com.resumeanalyzer.ai.orchestration;

import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.cache.AiResponseCache;
import com.resumeanalyzer.ai.model.ChatTurn;
import com.resumeanalyzer.ai.observability.AiInvocation;
import com.resumeanalyzer.ai.observability.AiPricingProperties;
import com.resumeanalyzer.ai.observability.AiTelemetry;
import com.resumeanalyzer.ai.observability.TokenEstimator;
import com.resumeanalyzer.ai.prompt.PromptTemplate;
import com.resumeanalyzer.ai.support.Hashing;
import com.resumeanalyzer.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Cross-cutting wrapper for grounded LLM calls. Every call routed through here gets, uniformly:
 *
 * <ul>
 *   <li><b>caching</b> — identical (prompt-version + rendered-conversation) requests are served
 *       from {@link AiResponseCache} at zero cost/latency;</li>
 *   <li><b>cost tracking</b> — input/output tokens are estimated and priced per active model;</li>
 *   <li><b>observability</b> — an {@link AiInvocation} is recorded for every call (cache hit or
 *       miss), carrying prompt version, latency, tokens, cost, and retrieval stats.</li>
 * </ul>
 *
 * <p>Returns the raw model text; callers own parsing. Keeping this orthogonal to prompt assembly
 * lets both the analysis and interview orchestrators share one audited execution path.</p>
 */
@Service
@RequiredArgsConstructor
public class GroundedLlmExecutor {

    private final AiProviderResolver aiProviderResolver;
    private final AiResponseCache cache;
    private final AiTelemetry telemetry;
    private final TokenEstimator tokenEstimator;
    private final AiPricingProperties pricing;
    private final AppProperties appProperties;

    /**
     * Executes a grounded prompt with caching, cost, and telemetry.
     *
     * @param operation        telemetry operation name (e.g. {@code "analysis.explain"})
     * @param template         the versioned prompt template (for the recorded prompt id)
     * @param conversation     the fully rendered system+user turns
     * @param retrievedChunks  number of RAG chunks that grounded this call (for observability)
     * @param topSimilarity    best retrieval similarity (for observability)
     * @return the raw model text
     */
    public String execute(String operation, PromptTemplate template, List<ChatTurn> conversation,
                          int retrievedChunks, double topSimilarity) {
        String provider = aiProviderResolver.current().name();
        String model = activeModel(provider);
        String cacheKey = Hashing.key(template.fullId(), "|", provider, "|", flatten(conversation));

        Optional<String> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            telemetry.record(AiInvocation.builder(operation)
                    .promptId(template.fullId()).provider(provider).model(model)
                    .cacheHit(true).retrievedChunks(retrievedChunks).topSimilarity(topSimilarity)
                    .build());
            return cached.get();
        }

        long start = System.nanoTime();
        String raw = aiProviderResolver.current().generate(conversation);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        int inputTokens = tokenEstimator.estimateConversation(conversation);
        int outputTokens = tokenEstimator.estimate(raw);
        double cost = pricing.inputCost(model, inputTokens) + pricing.outputCost(model, outputTokens);

        cache.put(cacheKey, raw);
        telemetry.record(AiInvocation.builder(operation)
                .promptId(template.fullId()).provider(provider).model(model)
                .latencyMs(latencyMs).inputTokens(inputTokens).outputTokens(outputTokens)
                .costUsd(cost).retrievedChunks(retrievedChunks).topSimilarity(topSimilarity)
                .build());
        return raw;
    }

    /** Records a deterministic (no-LLM) fallback so fallback rate is observable. */
    public void recordFallback(String operation, int retrievedChunks, double topSimilarity) {
        telemetry.record(AiInvocation.builder(operation)
                .provider(aiProviderResolver.current().name())
                .fallback(true).retrievedChunks(retrievedChunks).topSimilarity(topSimilarity)
                .build());
    }

    private String activeModel(String provider) {
        AppProperties.Ai ai = appProperties.ai();
        if (ai == null) {
            return provider;
        }
        return switch (provider) {
            case "openai" -> ai.openai() != null ? ai.openai().model() : provider;
            case "gemini" -> ai.gemini() != null ? ai.gemini().model() : provider;
            default -> provider;
        };
    }

    private String flatten(List<ChatTurn> conversation) {
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : conversation) {
            sb.append(turn.role()).append(':').append(turn.content()).append('\n');
        }
        return sb.toString();
    }
}
