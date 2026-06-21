package com.resumeanalyzer.ai.support;

/**
 * Centralised prompt templates. Keeping prompts in one place makes them reviewable,
 * testable, and reusable across providers. Every prompt instructs the model to return
 * strict JSON matching the corresponding model record so responses parse deterministically.
 */
public final class AiPrompts {

    private AiPrompts() {
    }

    public static final String RESUME_SYSTEM = """
            You are an expert technical recruiter and resume parser. Extract structured data
            from the resume text. Respond with ONLY a JSON object, no markdown, matching:
            {
              "name": string,
              "email": string,
              "phone": string,
              "summary": string,
              "skills": string[],
              "education": string[],
              "experience": string[],
              "certifications": string[],
              "projects": string[]
            }
            Use empty strings/arrays when a field is absent.""";

    public static final String JD_SYSTEM = """
            You are an expert recruiter. Analyse the job description and respond with ONLY a
            JSON object, no markdown, matching:
            {
              "requiredSkills": string[],
              "preferredSkills": string[],
              "keywords": string[],
              "experienceYears": number
            }""";

    public static final String SKILL_GAP_SYSTEM = """
            You are an ATS (Applicant Tracking System) and career coach. Compare the RESUME
            against the JOB DESCRIPTION. Respond with ONLY a JSON object, no markdown, matching:
            {
              "matchScore": number (0-100),
              "skillMatchScore": number (0-100),
              "experienceMatchScore": number (0-100),
              "educationMatchScore": number (0-100),
              "keywordMatchScore": number (0-100),
              "missingSkills": string[],
              "strengths": string[],
              "weaknesses": string[],
              "recommendations": string[]
            }
            Be specific and actionable. Base scores on genuine overlap.""";

    public static final String QUESTIONS_SYSTEM = """
            You are a senior interviewer. Generate interview questions tailored to the
            candidate's resume and the target job. Respond with ONLY a JSON array, no markdown,
            of objects matching:
            { "category": "TECHNICAL"|"BEHAVIORAL"|"HR"|"SYSTEM_DESIGN",
              "difficulty": "EASY"|"MEDIUM"|"HARD",
              "question": string }
            Provide a balanced mix across categories and difficulties.""";

    public static final String INTERVIEW_SYSTEM = """
            You are a friendly but rigorous technical interviewer conducting a mock interview.
            Ask ONE question at a time. Keep messages concise and conversational. Tailor
            questions to the candidate's resume and the target role.""";

    public static final String EVALUATE_SYSTEM = """
            You are an interviewer evaluating the candidate's most recent answer in context of
            the conversation. Respond with ONLY a JSON object, no markdown, matching:
            { "score": number (0-100),
              "feedback": string (one or two sentences of constructive critique),
              "nextMessage": string (your next interviewer turn: usually the next question) }""";

    public static final String FEEDBACK_SYSTEM = """
            You are an interview coach producing a final assessment of the whole conversation.
            Respond with ONLY a JSON object, no markdown, matching:
            { "communicationScore": number (0-100),
              "technicalScore": number (0-100),
              "confidenceScore": number (0-100),
              "overallScore": number (0-100),
              "improvementAreas": string[],
              "summary": string }""";

    public static String skillGapUser(String resume, String jd) {
        return "RESUME:\n%s\n\nJOB DESCRIPTION:\n%s".formatted(truncate(resume), truncate(jd));
    }

    public static String questionsUser(String resume, String jd, int count) {
        return "Generate exactly %d questions.\n\nRESUME:\n%s\n\nJOB DESCRIPTION:\n%s"
                .formatted(count, truncate(resume), truncate(jd));
    }

    /** Guards against oversized prompts blowing the model context / cost budget. */
    public static String truncate(String text) {
        if (text == null) {
            return "";
        }
        int max = 8000;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
