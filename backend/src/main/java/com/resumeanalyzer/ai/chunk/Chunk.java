package com.resumeanalyzer.ai.chunk;

import java.util.Set;

/**
 * A semantically-bounded slice of a resume with its provenance metadata. Chunks never cross
 * section boundaries and never split a bullet/sentence, so each is self-contained context
 * for retrieval.
 */
public record Chunk(
        String section,
        int index,
        String content,
        Set<String> skills
) {}
