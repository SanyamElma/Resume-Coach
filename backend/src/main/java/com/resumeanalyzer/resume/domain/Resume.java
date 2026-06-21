package com.resumeanalyzer.resume.domain;

import com.resumeanalyzer.common.domain.Auditable;
import com.resumeanalyzer.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * An uploaded resume document and its extracted text. Supports version tracking via the
 * {@code version} column (bonus feature) — successive uploads keyed to the same logical
 * resume increment the version.
 */
@Entity
@Table(name = "resumes", indexes = @Index(name = "idx_resumes_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resume_name", nullable = false, length = 255)
    private String resumeName;

    /** Storage key (local path or future S3 object key) — not the public URL. */
    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "parsed_text", columnDefinition = "text")
    private String parsedText;

    /** JSON blob of structured fields extracted by {@code ResumeParserService}. */
    @Column(name = "parsed_data", columnDefinition = "text")
    private String parsedData;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;
}
