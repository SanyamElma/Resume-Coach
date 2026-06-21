package com.resumeanalyzer.ai.model;

import java.util.List;

/** Structured fields extracted from raw resume text. */
public record ParsedResume(
        String name,
        String email,
        String phone,
        String summary,
        List<String> skills,
        List<String> education,
        List<String> experience,
        List<String> certifications,
        List<String> projects
) {}
