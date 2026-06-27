package com.resumeanalyzer.ai.text;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Normalises raw extracted resume text before any downstream processing.
 *
 * <p>Removes hidden/control characters, collapses broken whitespace, strips page
 * numbers and repeated header/footer lines (a common PDF artefact), and de-duplicates
 * consecutive identical lines — producing clean text that improves both deterministic
 * parsing accuracy and embedding quality.</p>
 */
@Component
public class ResumeCleaner {

    // Zero-width and other invisible/control characters (keep \n and \t handling separate).
    private static final Pattern HIDDEN = Pattern.compile("[\\u200B-\\u200D\\uFEFF\\u00AD\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]");
    private static final Pattern PAGE_MARKER = Pattern.compile(
            "(?i)^\\s*(page\\s*\\d+\\s*(of\\s*\\d+)?|\\d+\\s*/\\s*\\d+|-\\s*\\d+\\s*-)\\s*$");
    private static final Pattern MULTI_BLANK = Pattern.compile("\\n{3,}");
    private static final Pattern TRAILING_SPACES = Pattern.compile("[ \\t]+\\n");

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = HIDDEN.matcher(raw).replaceAll("");
        text = text.replace("\r\n", "\n").replace("\r", "\n");

        String[] lines = text.split("\n", -1);

        // Identify repeated short lines (headers/footers reprinted on every page).
        Map<String, Integer> frequency = new HashMap<>();
        for (String line : lines) {
            String key = line.trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && key.length() <= 60) {
                frequency.merge(key, 1, Integer::sum);
            }
        }

        List<String> kept = new ArrayList<>();
        String previous = null;
        for (String line : lines) {
            String trimmed = line.trim();
            String key = trimmed.toLowerCase(Locale.ROOT);

            if (PAGE_MARKER.matcher(line).matches()) {
                continue;
            }
            // Drop short lines that repeat 3+ times (running headers/footers).
            if (!key.isEmpty() && key.length() <= 60 && frequency.getOrDefault(key, 0) >= 3) {
                continue;
            }
            // Collapse consecutive duplicate lines.
            if (trimmed.equals(previous) && !trimmed.isEmpty()) {
                continue;
            }
            kept.add(line.replaceAll("[ \\t]{2,}", " ").stripTrailing());
            previous = trimmed;
        }

        String result = String.join("\n", kept);
        result = TRAILING_SPACES.matcher(result).replaceAll("\n");
        result = MULTI_BLANK.matcher(result).replaceAll("\n\n");
        return result.strip();
    }
}
