package com.resumeanalyzer.ai.experience;

import com.resumeanalyzer.ai.skill.SkillExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes professional experience deterministically from date ranges in resume text.
 *
 * <p>The LLM is never asked to do arithmetic. This engine parses date ranges
 * (e.g. "Jan 2020 – Present", "06/2018 - 09/2020", "2019 to 2021"), unions overlapping
 * intervals to avoid double-counting, and attributes each role's duration to the skills
 * mentioned within that role's text block — yielding total and per-skill years of experience.</p>
 */
@Component
@RequiredArgsConstructor
public class ExperienceCalculator {

    private static final Map<String, Integer> MONTHS = buildMonthMap();

    // A single date token: "Jan 2020" / "January 2020" / "06/2018" / "2019"
    private static final String DATE = "(?:[A-Za-z]{3,9}\\.?\\s+\\d{4}|\\d{1,2}/\\d{4}|\\d{4})";
    private static final String END = "(?:" + DATE + "|present|current|now|till\\s*date|to\\s*date)";

    private static final Pattern RANGE = Pattern.compile(
            "(" + DATE + ")\\s*(?:-|–|—|to|until)\\s*(" + END + ")",
            Pattern.CASE_INSENSITIVE);

    private final SkillExtractor skillExtractor;

    public ExperienceResult calculate(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return new ExperienceResult(0.0, Map.of());
        }

        record Block(int monthStart, int monthEnd, String window) {}
        List<Block> blocks = new ArrayList<>();
        Matcher matcher = RANGE.matcher(resumeText);

        List<int[]> matchSpans = new ArrayList<>();
        List<YearMonth[]> parsed = new ArrayList<>();
        while (matcher.find()) {
            YearMonth start = parseDate(matcher.group(1), false);
            YearMonth end = parseEnd(matcher.group(2));
            if (start == null || end == null || end.isBefore(start)) {
                continue;
            }
            matchSpans.add(new int[]{matcher.start(), matcher.end()});
            parsed.add(new YearMonth[]{start, end});
        }

        for (int i = 0; i < parsed.size(); i++) {
            int windowStart = matchSpans.get(i)[0];
            int windowEnd = (i + 1 < matchSpans.size()) ? matchSpans.get(i + 1)[0]
                    : Math.min(resumeText.length(), matchSpans.get(i)[1] + 400);
            String window = resumeText.substring(windowStart, windowEnd);
            blocks.add(new Block(epochMonth(parsed.get(i)[0]), epochMonth(parsed.get(i)[1]), window));
        }

        // Total experience: union of all role intervals.
        List<int[]> allIntervals = blocks.stream().map(b -> new int[]{b.monthStart(), b.monthEnd()}).toList();
        double totalYears = round(unionMonths(allIntervals) / 12.0);

        // Per-skill: union of intervals of the blocks each skill appears in.
        Map<String, List<int[]>> perSkillIntervals = new HashMap<>();
        for (Block block : blocks) {
            for (String skill : skillExtractor.extract(block.window())) {
                perSkillIntervals.computeIfAbsent(skill, k -> new ArrayList<>())
                        .add(new int[]{block.monthStart(), block.monthEnd()});
            }
        }
        Map<String, Double> perSkillYears = new HashMap<>();
        perSkillIntervals.forEach((skill, intervals) ->
                perSkillYears.put(skill, round(unionMonths(intervals) / 12.0)));

        return new ExperienceResult(totalYears, perSkillYears);
    }

    /** Inclusive month-count of the union of [startEpochMonth, endEpochMonth] intervals. */
    private int unionMonths(List<int[]> intervals) {
        if (intervals.isEmpty()) {
            return 0;
        }
        List<int[]> sorted = new ArrayList<>(intervals);
        sorted.sort((a, b) -> Integer.compare(a[0], b[0]));
        int total = 0;
        int curStart = sorted.get(0)[0];
        int curEnd = sorted.get(0)[1];
        for (int i = 1; i < sorted.size(); i++) {
            int[] next = sorted.get(i);
            if (next[0] <= curEnd + 1) {
                curEnd = Math.max(curEnd, next[1]);
            } else {
                total += (curEnd - curStart + 1);
                curStart = next[0];
                curEnd = next[1];
            }
        }
        total += (curEnd - curStart + 1);
        return total;
    }

    private int epochMonth(YearMonth ym) {
        return ym.getYear() * 12 + (ym.getMonthValue() - 1);
    }

    private YearMonth parseEnd(String token) {
        String lower = token.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("present") || lower.startsWith("current") || lower.startsWith("now")
                || lower.contains("date")) {
            return YearMonth.now();
        }
        return parseDate(token, true);
    }

    private YearMonth parseDate(String token, boolean isEnd) {
        String t = token.trim();
        try {
            if (t.matches("\\d{1,2}/\\d{4}")) {
                String[] parts = t.split("/");
                return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            if (t.matches("\\d{4}")) {
                return YearMonth.of(Integer.parseInt(t), isEnd ? 12 : 1);
            }
            Matcher m = Pattern.compile("([A-Za-z]{3,9})\\.?\\s+(\\d{4})").matcher(t);
            if (m.find()) {
                Integer month = MONTHS.get(m.group(1).toLowerCase(Locale.ROOT).substring(0, 3));
                if (month != null) {
                    return YearMonth.of(Integer.parseInt(m.group(2)), month);
                }
            }
        } catch (Exception ignored) {
            // fall through to null on malformed input
        }
        return null;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static Map<String, Integer> buildMonthMap() {
        String[] keys = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], i + 1);
        }
        return map;
    }
}
