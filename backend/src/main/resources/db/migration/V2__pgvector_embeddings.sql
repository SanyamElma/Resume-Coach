-- =====================================================================
-- V2: pgvector-backed RAG store for resume chunk embeddings.
-- Additive only — does not touch existing tables. Runs under PostgreSQL
-- (Flyway profiles); the offline H2 profile uses the in-memory store instead.
-- The vector dimension (768) matches app.ai.engine.embedding-dimensions.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE resume_chunk_embeddings (
    id                 UUID PRIMARY KEY,
    resume_id          UUID         NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    user_id            UUID         NOT NULL,
    section            VARCHAR(40),
    chunk_index        INTEGER      NOT NULL,
    content            TEXT         NOT NULL,
    metadata           TEXT,
    embedding_provider VARCHAR(40)  NOT NULL,
    embedding_model    VARCHAR(80)  NOT NULL,
    content_hash       VARCHAR(80)  NOT NULL,
    embedding          vector(768)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunk_resume ON resume_chunk_embeddings (resume_id);
CREATE INDEX idx_chunk_provider ON resume_chunk_embeddings (embedding_provider, embedding_model);

-- Approximate-nearest-neighbour index for cosine similarity search.
CREATE INDEX idx_chunk_embedding_cosine
    ON resume_chunk_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
