package com.resumeanalyzer.job.domain;

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
 * A job description supplied by a candidate, optionally enriched by the AI provider with
 * structured fields (required skills, preferred skills, keywords) stored as JSON.
 */
@Entity
@Table(name = "job_descriptions", indexes = @Index(name = "idx_jd_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    /** AI-structured JSON: requiredSkills, preferredSkills, keywords, experienceYears. */
    @Column(name = "structured_data", columnDefinition = "text")
    private String structuredData;
}
