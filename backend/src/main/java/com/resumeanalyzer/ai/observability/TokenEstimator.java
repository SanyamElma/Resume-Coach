package com.resumeanalyzer.ai.observability;

import com.resumeanalyzer.ai.model.ChatTurn;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider-agnostic token estimator. Real tokenizers (tiktoken/SentencePiece) are model-specific
 * and not on the classpath; for cost tracking and budgeting a stable heuristic is sufficient and
 * deliberately conservative (it slightly over-estimates so budgets are never silently exceeded).
 *
 * <p>The estimate blends a character-based (~4 chars/token) and a word-based (~1.3 tokens/word)
 * approximation and takes the larger, which tracks real GPT/Gemini tokenization within a small
 * margin across natural-language and code-like text.</p>
 */
@Component
public class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;
    private static final double TOKENS_PER_WORD = 1.3;
    private static final int PER_MESSAGE_OVERHEAD = 4; // role/formatting framing per chat turn

    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int chars = text.length();
        int words = text.trim().split("\\s+").length;
        int byChars = (int) Math.ceil(chars / CHARS_PER_TOKEN);
        int byWords = (int) Math.ceil(words * TOKENS_PER_WORD);
        return Math.max(byChars, byWords);
    }

    public int estimateConversation(List<ChatTurn> conversation) {
        if (conversation == null || conversation.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ChatTurn turn : conversation) {
            total += estimate(turn.content()) + PER_MESSAGE_OVERHEAD;
        }
        return total;
    }
}
