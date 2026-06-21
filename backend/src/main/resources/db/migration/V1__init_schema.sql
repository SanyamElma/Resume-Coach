-- =====================================================================
-- V1: Initial schema for AI Resume Analyzer & Interview Coach
-- Source of truth for the database; Hibernate runs in `validate` mode.
-- Instant columns use timestamptz to match Hibernate's TIMESTAMP_UTC mapping.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------- users --------------------------------
CREATE TABLE users (
    id         UUID PRIMARY KEY,
    name       VARCHAR(120)  NOT NULL,
    email      VARCHAR(180)  NOT NULL,
    password   VARCHAR(255)  NOT NULL,
    role       VARCHAR(20)   NOT NULL DEFAULT 'CANDIDATE',
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_users_email ON users (email);

-- ------------------------- refresh_tokens ---------------------------
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX idx_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

-- ---------------------- password_reset_tokens -----------------------
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX idx_reset_token_hash ON password_reset_tokens (token_hash);

-- ----------------------------- resumes ------------------------------
CREATE TABLE resumes (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    resume_name  VARCHAR(255) NOT NULL,
    file_path    VARCHAR(512) NOT NULL,
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    parsed_text  TEXT,
    parsed_data  TEXT,
    version      INTEGER      NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ
);
CREATE INDEX idx_resumes_user ON resumes (user_id);

-- ------------------------- job_descriptions -------------------------
CREATE TABLE job_descriptions (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    company         VARCHAR(200),
    description     TEXT         NOT NULL,
    structured_data TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ
);
CREATE INDEX idx_jd_user ON job_descriptions (user_id);

-- ------------------------- analysis_reports -------------------------
CREATE TABLE analysis_reports (
    id                     UUID PRIMARY KEY,
    resume_id              UUID        NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    job_description_id     UUID        NOT NULL REFERENCES job_descriptions (id) ON DELETE CASCADE,
    match_percentage       INTEGER     NOT NULL,
    skill_match_score      INTEGER,
    experience_match_score INTEGER,
    education_match_score  INTEGER,
    keyword_match_score    INTEGER,
    missing_skills         TEXT,
    strengths              TEXT,
    weaknesses             TEXT,
    recommendations        TEXT,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ
);
CREATE INDEX idx_analysis_resume ON analysis_reports (resume_id);
CREATE INDEX idx_analysis_jd ON analysis_reports (job_description_id);

-- ------------------------ interview_sessions ------------------------
CREATE TABLE interview_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_description_id  UUID        REFERENCES job_descriptions (id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    score               INTEGER,
    communication_score INTEGER,
    technical_score     INTEGER,
    confidence_score    INTEGER,
    feedback            TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ
);
CREATE INDEX idx_session_user ON interview_sessions (user_id);

-- ------------------------ interview_messages ------------------------
CREATE TABLE interview_messages (
    id           UUID PRIMARY KEY,
    session_id   UUID        NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    sender       VARCHAR(10) NOT NULL,
    message      TEXT        NOT NULL,
    answer_score INTEGER,
    created_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_message_session ON interview_messages (session_id);
