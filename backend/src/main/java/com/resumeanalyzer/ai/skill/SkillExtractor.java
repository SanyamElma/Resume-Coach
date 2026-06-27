package com.resumeanalyzer.ai.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministically extracts canonical skills from free text using the
 * {@link SkillDictionary}. Surface forms are matched with alphanumeric word boundaries so
 * short tokens like "go", "js", or "es" do not produce false positives inside other words
 * (e.g. "google", "jstor"). Every match is resolved to its canonical skill.
 *
 * <p>This replaces naive {@code text.contains(skill)} scanning and is the grounding layer
 * the LLM never sees raw — it only explains the structured result.</p>
 */
@Component
@RequiredArgsConstructor
public class SkillExtractor {

    private final SkillDictionary dictionary;

    /** Returns the set of canonical skills present in the text (insertion-ordered). */
    public Set<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String form : dictionary.allSurfaceForms()) {
            if (containsAsToken(haystack, form)) {
                dictionary.normalize(form).ifPresent(s -> found.add(s.canonical()));
            }
        }
        return found;
    }

    /** True if {@code form} appears in {@code haystack} bounded by non-alphanumeric chars. */
    private boolean containsAsToken(String haystack, String form) {
        // Fast pre-check before the regex.
        if (!haystack.contains(form)) {
            return false;
        }
        Pattern pattern = Pattern.compile(
                "(?<![A-Za-z0-9])" + Pattern.quote(form) + "(?![A-Za-z0-9])");
        Matcher matcher = pattern.matcher(haystack);
        return matcher.find();
    }
}
