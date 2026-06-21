package com.resumeanalyzer.analysis.domain;

import com.resumeanalyzer.common.domain.Auditable;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.resume.domain.Resume;
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
 * The result of comparing a {@link Resume} against a {@link JobDescription}. List-valued
 * fields (missing skills, strengths, etc.) are persisted as JSON arrays for portability;
 * the sub-scores back the ATS score dashboard.
 */
@Entity
@Table(name = "analysis_reports", indexes = {
        @Index(name = "idx_analysis_resume", columnList = "resume_id"),
        @Index(name = "idx_analysis_jd", columnList = "job_description_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(name = "match_percentage", nullable = false)
    private int matchPercentage;

    @Column(name = "skill_match_score")
    private Integer skillMatchScore;

    @Column(name = "experience_match_score")
    private Integer experienceMatchScore;

    @Column(name = "education_match_score")
    private Integer educationMatchScore;

    @Column(name = "keyword_match_score")
    private Integer keywordMatchScore;

    @Column(name = "missing_skills", columnDefinition = "text")
    private String missingSkills;

    @Column(name = "strengths", columnDefinition = "text")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "text")
    private String weaknesses;

    @Column(name = "recommendations", columnDefinition = "text")
    private String recommendations;
}
