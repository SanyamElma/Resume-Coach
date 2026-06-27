package com.resumeanalyzer.ai.chunk;

import com.resumeanalyzer.ai.section.ResumeSections;
import com.resumeanalyzer.ai.skill.SkillExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits resume sections into semantic chunks (NOT fixed character counts).
 *
 * <p>Within each section, content is broken into units (bullets/lines, further split by
 * sentence when a unit is oversized), then greedily packed into chunks up to a target size
 * without ever splitting a unit. Section boundaries are hard boundaries, and each chunk is
 * tagged with the canonical skills it mentions — the metadata required for filtered retrieval.</p>
 */
@Component
@RequiredArgsConstructor
public class ChunkingEngine {

    private static final int TARGET_CHARS = 480;
    private static final int MAX_UNIT_CHARS = 600;

    private final SkillExtractor skillExtractor;

    public List<Chunk> chunk(ResumeSections sections) {
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        for (var entry : sections.sections().entrySet()) {
            String sectionName = entry.getKey().name();
            for (String content : packUnits(splitUnits(entry.getValue()))) {
                chunks.add(new Chunk(sectionName, index++, content, skillExtractor.extract(content)));
            }
        }
        return chunks;
    }

    /** Splits section text into atomic units (bullets/lines), splitting oversized ones by sentence. */
    private List<String> splitUnits(String sectionText) {
        List<String> units = new ArrayList<>();
        for (String line : sectionText.split("\\r?\\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= MAX_UNIT_CHARS) {
                units.add(trimmed);
            } else {
                for (String sentence : trimmed.split("(?<=[.!?])\\s+")) {
                    if (!sentence.isBlank()) {
                        units.add(sentence.strip());
                    }
                }
            }
        }
        return units;
    }

    /** Greedily packs units into chunks up to {@link #TARGET_CHARS} without splitting a unit. */
    private List<String> packUnits(List<String> units) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.length() > 0 && current.length() + unit.length() + 1 > TARGET_CHARS) {
                chunks.add(current.toString().strip());
                current.setLength(0);
            }
            current.append(unit).append('\n');
        }
        if (current.length() > 0) {
            chunks.add(current.toString().strip());
        }
        return chunks;
    }
}
