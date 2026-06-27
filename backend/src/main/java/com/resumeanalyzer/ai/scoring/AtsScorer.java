package com.resumeanalyzer.ai.scoring;

import com.resumeanalyzer.ai.section.ResumeSections;
import com.resumeanalyzer.ai.section.ResumeSections.SectionType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Computes a weighted, multi-dimension ATS score deterministically. Each dimension is a
 * 0-100 sub-score; the overall score is their fixed-weight combination. No randomness, no
 * LLM — the LLM is later asked only to explain and suggest improvements.
 */
@Component
public class AtsScorer {

    private static final Map<String, Double> WEIGHTS = buildWeights();

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s().-]{7,}\\d");
    private static final Pattern DEGREE = Pattern.compile(
            "(?i)\\b(bachelor|master|ph\\.?d|b\\.?tech|b\\.?e\\b|b\\.?sc|m\\.?tech|m\\.?sc|mca|bca|mba|degree)\\b");
    private static final Pattern QUANTIFIED = Pattern.compile(
            "(\\d+%|\\$\\d+|\\b\\d{2,}\\b.{0,30}(users|requests|customers|revenue|latency|users|hours|days|x\\b))",
            Pattern.CASE_INSENSITIVE);

    public AtsBreakdown score(AtsSignals signals) {
        ResumeSections sections = signals.sections();
        String text = signals.resumeText() == null ? "" : signals.resumeText();

        int skills = clamp(signals.skillMatchScore());
        int experience = clamp(signals.experienceMatchScore());
        int keywords = clamp(signals.keywordMatchScore());
        int projects = scoreProjects(sections);
        int education = scoreEducation(sections, text);
        int certifications = scoreCertifications(sections);
        int achievements = scoreAchievements(sections, text);
        int formatting = scoreFormatting(sections, text);

        double overall =
                WEIGHTS.get("skills") * skills
                        + WEIGHTS.get("experience") * experience
                        + WEIGHTS.get("keywords") * keywords
                        + WEIGHTS.get("projects") * projects
                        + WEIGHTS.get("education") * education
                        + WEIGHTS.get("certifications") * certifications
                        + WEIGHTS.get("achievements") * achievements
                        + WEIGHTS.get("formatting") * formatting;

        return new AtsBreakdown(clamp((int) Math.round(overall)), skills, experience, keywords,
                projects, education, certifications, achievements, formatting, WEIGHTS);
    }

    private int scoreProjects(ResumeSections sections) {
        if (!sections.has(SectionType.PROJECTS)) {
            return 40;
        }
        long bullets = sections.get(SectionType.PROJECTS).lines().filter(l -> !l.isBlank()).count();
        return clamp((int) (55 + bullets * 8));
    }

    private int scoreEducation(ResumeSections sections, String text) {
        String haystack = (sections.get(SectionType.EDUCATION) + " " + text);
        boolean hasSection = sections.has(SectionType.EDUCATION);
        if (DEGREE.matcher(haystack).find()) {
            String lower = haystack.toLowerCase(Locale.ROOT);
            if (lower.contains("master") || lower.contains("phd") || lower.contains("m.tech")) {
                return 100;
            }
            return 88;
        }
        return hasSection ? 60 : 40;
    }

    private int scoreCertifications(ResumeSections sections) {
        return sections.has(SectionType.CERTIFICATIONS) ? 100 : 55;
    }

    private int scoreAchievements(ResumeSections sections, String text) {
        long quantified = QUANTIFIED.matcher(text).results().count();
        int base = sections.has(SectionType.ACHIEVEMENTS) ? 60 : 40;
        return clamp((int) (base + quantified * 12));
    }

    private int scoreFormatting(ResumeSections sections, String text) {
        int score = 100;
        if (!EMAIL.matcher(text).find()) {
            score -= 20;
        }
        if (!PHONE.matcher(text).find()) {
            score -= 10;
        }
        long distinctSections = sections.sections().keySet().stream()
                .filter(t -> t != SectionType.OTHER).count();
        if (distinctSections < 4) {
            score -= 20;
        }
        int words = text.isBlank() ? 0 : text.split("\\s+").length;
        if (words < 150 || words > 1200) {
            score -= 15;
        }
        return clamp(score);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static Map<String, Double> buildWeights() {
        Map<String, Double> w = new LinkedHashMap<>();
        w.put("skills", 0.30);
        w.put("experience", 0.20);
        w.put("keywords", 0.15);
        w.put("projects", 0.10);
        w.put("achievements", 0.07);
        w.put("education", 0.08);
        w.put("certifications", 0.05);
        w.put("formatting", 0.05);
        return w;
    }
}
