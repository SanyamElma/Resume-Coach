package com.resumeanalyzer.ai.jd;

import com.resumeanalyzer.ai.skill.SkillExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministically parses a job description into structured requirements. Required vs.
 * preferred skills are separated by detecting "nice to have / preferred / bonus" regions;
 * years-of-experience and seniority are extracted via regex/keyword rules.
 */
@Component
@RequiredArgsConstructor
public class JobRequirementExtractor {

    private static final Pattern PREFERRED_MARKER = Pattern.compile(
            "(?i)(nice to have|preferred|bonus|plus|good to have|desirable|advantageous)");
    private static final Pattern YEARS = Pattern.compile(
            "(\\d{1,2})\\+?\\s*(?:years|yrs)", Pattern.CASE_INSENSITIVE);

    private final SkillExtractor skillExtractor;

    public JobRequirements extract(String jdText) {
        String text = jdText == null ? "" : jdText;

        int preferredIdx = firstPreferredIndex(text);
        String requiredRegion = preferredIdx >= 0 ? text.substring(0, preferredIdx) : text;
        String preferredRegion = preferredIdx >= 0 ? text.substring(preferredIdx) : "";

        Set<String> required = new LinkedHashSet<>(skillExtractor.extract(requiredRegion));
        Set<String> preferred = new LinkedHashSet<>(skillExtractor.extract(preferredRegion));
        preferred.removeAll(required); // required wins on overlap

        Set<String> keywords = new LinkedHashSet<>(required);
        keywords.addAll(preferred);

        return new JobRequirements(
                new ArrayList<>(required),
                new ArrayList<>(preferred),
                new ArrayList<>(keywords),
                minYears(text),
                seniority(text));
    }

    private int firstPreferredIndex(String text) {
        Matcher m = PREFERRED_MARKER.matcher(text);
        return m.find() ? m.start() : -1;
    }

    private Integer minYears(String text) {
        Matcher m = YEARS.matcher(text);
        Integer min = null;
        while (m.find()) {
            int years = Integer.parseInt(m.group(1));
            if (years <= 40 && (min == null || years < min)) {
                min = years;
            }
        }
        return min;
    }

    private JobRequirements.Seniority seniority(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("principal") || lower.contains("staff") || lower.contains("lead")
                || lower.contains("architect")) {
            return JobRequirements.Seniority.LEAD;
        }
        if (lower.contains("senior") || lower.contains("sr.") || lower.contains("sr ")) {
            return JobRequirements.Seniority.SENIOR;
        }
        if (lower.contains("junior") || lower.contains("entry") || lower.contains("graduate")
                || lower.contains("intern")) {
            return JobRequirements.Seniority.JUNIOR;
        }
        return JobRequirements.Seniority.MID;
    }
}
