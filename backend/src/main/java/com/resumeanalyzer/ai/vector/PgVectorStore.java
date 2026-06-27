package com.resumeanalyzer.ai.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL + pgvector {@link VectorStore} for production. Uses cosine distance
 * ({@code <=>} with {@code vector_cosine_ops}) for ANN search. Activated when
 * {@code app.ai.engine.vector-store=pgvector} (the default). The backing table and the
 * {@code vector} extension are provisioned by the Flyway migration {@code V2}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.engine.vector-store", havingValue = "pgvector", matchIfMissing = true)
public class PgVectorStore implements VectorStore {

    private static final String TABLE = "resume_chunk_embeddings";

    private final JdbcTemplate jdbc;

    public PgVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        log.info("VectorStore: PostgreSQL/pgvector");
    }

    @Override
    public void replaceResumeChunks(UUID resumeId, List<VectorRecord> records) {
        jdbc.update("DELETE FROM " + TABLE + " WHERE resume_id = ?", resumeId);
        if (records.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + TABLE + " (id, resume_id, user_id, section, chunk_index, content, "
                + "metadata, embedding_provider, embedding_model, content_hash, embedding) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector)";
        for (VectorRecord r : records) {
            jdbc.update(sql, r.id(), r.resumeId(), r.userId(), r.section(), r.chunkIndex(), r.content(),
                    r.metadataJson(), r.provider(), r.model(), r.contentHash(), toVectorLiteral(r.embedding()));
        }
    }

    @Override
    public List<ScoredChunk> search(VectorSearchRequest req) {
        String vectorLiteral = toVectorLiteral(req.queryEmbedding());
        List<Object> args = new ArrayList<>();

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (req.resumeId() != null) {
            where.append(" AND resume_id = ?");
            args.add(req.resumeId());
        }
        if (req.userId() != null) {
            where.append(" AND user_id = ?");
            args.add(req.userId());
        }
        if (req.provider() != null) {
            where.append(" AND embedding_provider = ?");
            args.add(req.provider());
        }
        if (req.model() != null) {
            where.append(" AND embedding_model = ?");
            args.add(req.model());
        }
        if (req.sections() != null && !req.sections().isEmpty()) {
            where.append(" AND section = ANY(?)");
            args.add(req.sections().toArray(String[]::new));
        }

        // score = cosine similarity = 1 - cosine distance
        String sql = "SELECT id, resume_id, section, content, metadata, "
                + "1 - (embedding <=> ?::vector) AS score FROM " + TABLE + where
                + " AND (1 - (embedding <=> ?::vector)) >= ? ORDER BY embedding <=> ?::vector LIMIT ?";

        List<Object> finalArgs = new ArrayList<>();
        finalArgs.add(vectorLiteral);              // score select
        finalArgs.addAll(args);                    // filters
        finalArgs.add(vectorLiteral);              // threshold compare
        finalArgs.add(req.threshold());
        finalArgs.add(vectorLiteral);              // order by
        finalArgs.add(Math.max(1, req.topK()));

        return jdbc.query(sql, (rs, rowNum) -> new ScoredChunk(
                rs.getObject("id", UUID.class),
                rs.getObject("resume_id", UUID.class),
                rs.getString("section"),
                rs.getString("content"),
                rs.getString("metadata"),
                rs.getDouble("score")), finalArgs.toArray());
    }

    @Override
    public void deleteByResume(UUID resumeId) {
        jdbc.update("DELETE FROM " + TABLE + " WHERE resume_id = ?", resumeId);
    }

    @Override
    public Optional<String> currentContentHash(UUID resumeId) {
        List<String> hashes = jdbc.query(
                "SELECT content_hash FROM " + TABLE + " WHERE resume_id = ? LIMIT 1",
                (rs, n) -> rs.getString(1), resumeId);
        return hashes.stream().findFirst();
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
