package com.resumeanalyzer.resume.service;

import com.resumeanalyzer.ai.model.ParsedResume;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic regex/keyword-based resume extractor used as a fallback when the AI
 * provider is unavailable. Keeps the upload pipeline functional and side-effect free.
 */
@Component
public class RegexResumeExtractor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");

    private static final List<String> SKILLS = List.of(
            "java", "spring", "spring boot", "hibernate", "react", "javascript", "typescript", "node",
            "python", "sql", "postgresql", "mysql", "mongodb", "docker", "kubernetes", "aws", "azure",
            "kafka", "rest", "graphql", "microservices", "git", "html", "css", "junit", "maven");

    public ParsedResume extract(String text) {
        String safe = text == null ? "" : text;
        return new ParsedResume(
                guessName(safe),
                firstMatch(EMAIL, safe),
                firstMatch(PHONE, safe),
                "",
                extractSkills(safe),
                linesContaining(safe, "education", "university", "bachelor", "master"),
                linesContaining(safe, "experience", "engineer", "developer"),
                linesContaining(safe, "certified", "certification"),
                linesContaining(safe, "project"));
    }

    private List<String> extractSkills(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : SKILLS) {
            if (lower.contains(skill)) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    private List<String> linesContaining(String text, String... keywords) {
        List<String> matches = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (String kw : keywords) {
                if (lower.contains(kw) && line.trim().length() < 200) {
                    matches.add(line.trim());
                    break;
                }
            }
        }
        return matches;
    }

    private String guessName(String text) {
        String first = text.strip().split("\\r?\\n", 2)[0].trim();
        return first.length() <= 60 && first.matches("[A-Za-z .'-]+") ? first : "";
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group().trim() : "";
    }
}
