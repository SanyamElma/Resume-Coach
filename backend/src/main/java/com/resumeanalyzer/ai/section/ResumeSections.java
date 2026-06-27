package com.resumeanalyzer.ai.section;

import java.util.Map;

/**
 * Resume content split into independently-addressable sections, keyed by a canonical
 * {@link SectionType} name. Downstream modules (chunking, ATS scoring, retrieval) operate
 * on individual sections rather than the whole document.
 */
public record ResumeSections(Map<SectionType, String> sections) {

    public enum SectionType {
        SUMMARY, EXPERIENCE, EDUCATION, SKILLS, PROJECTS,
        CERTIFICATIONS, ACHIEVEMENTS, LANGUAGES, OTHER
    }

    public boolean has(SectionType type) {
        String value = sections.get(type);
        return value != null && !value.isBlank();
    }

    public String get(SectionType type) {
        return sections.getOrDefault(type, "");
    }
}
