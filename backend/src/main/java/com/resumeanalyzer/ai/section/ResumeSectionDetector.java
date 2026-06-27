package com.resumeanalyzer.ai.section;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Splits cleaned resume text into canonical sections by recognising section headings.
 *
 * <p>A line is treated as a heading when, after trimming punctuation, it matches a known
 * heading synonym and looks like a heading (short, mostly letters). Content lines are
 * attributed to the most recent heading. This deterministic segmentation lets every other
 * module store and process sections independently, as required.</p>
 */
@Component
public class ResumeSectionDetector {

    private static final Map<ResumeSections.SectionType, List<String>> HEADINGS = Map.of(
            ResumeSections.SectionType.SUMMARY, List.of("summary", "objective", "profile", "about", "professional summary"),
            ResumeSections.SectionType.EXPERIENCE, List.of("experience", "work experience", "professional experience",
                    "employment", "work history", "employment history"),
            ResumeSections.SectionType.EDUCATION, List.of("education", "academic background", "academics"),
            ResumeSections.SectionType.SKILLS, List.of("skills", "technical skills", "technologies", "tech stack",
                    "core competencies", "skill set"),
            ResumeSections.SectionType.PROJECTS, List.of("projects", "personal projects", "key projects", "academic projects"),
            ResumeSections.SectionType.CERTIFICATIONS, List.of("certifications", "certificates", "licenses", "certification"),
            ResumeSections.SectionType.ACHIEVEMENTS, List.of("achievements", "accomplishments", "awards", "honors", "honours"),
            ResumeSections.SectionType.LANGUAGES, List.of("languages", "language proficiency")
    );

    public ResumeSections detect(String cleanedText) {
        Map<ResumeSections.SectionType, StringBuilder> buffers = new LinkedHashMap<>();
        if (cleanedText == null || cleanedText.isBlank()) {
            return new ResumeSections(Map.of());
        }

        ResumeSections.SectionType current = ResumeSections.SectionType.OTHER;
        for (String rawLine : cleanedText.split("\n", -1)) {
            ResumeSections.SectionType heading = matchHeading(rawLine);
            if (heading != null) {
                current = heading;
                buffers.computeIfAbsent(current, k -> new StringBuilder());
                continue;
            }
            if (!rawLine.isBlank()) {
                buffers.computeIfAbsent(current, k -> new StringBuilder())
                        .append(rawLine.strip()).append('\n');
            }
        }

        Map<ResumeSections.SectionType, String> sections = new LinkedHashMap<>();
        buffers.forEach((type, sb) -> {
            String content = sb.toString().strip();
            if (!content.isEmpty()) {
                sections.put(type, content);
            }
        });
        return new ResumeSections(sections);
    }

    private ResumeSections.SectionType matchHeading(String line) {
        String normalized = line.strip().replaceAll("[:•\\-_*]+$", "").strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 40 || normalized.matches(".*\\d{4}.*")) {
            return null;
        }
        // Headings are mostly letters/spaces (avoid matching sentences with punctuation).
        if (!normalized.matches("[a-z &/]+")) {
            return null;
        }
        for (var entry : HEADINGS.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
