package com.resumeanalyzer.interview.domain;

import com.resumeanalyzer.common.domain.Auditable;
import com.resumeanalyzer.job.domain.JobDescription;
import com.resumeanalyzer.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A mock interview conversation between the candidate and the AI interviewer. Aggregates
 * the per-dimension scores produced when the session is completed.
 */
@Entity
@Table(name = "interview_sessions", indexes = @Index(name = "idx_session_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    @Column(name = "score")
    private Integer score;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "feedback", columnDefinition = "text")
    private String feedback;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<InterviewMessage> messages = new ArrayList<>();

    public void addMessage(InterviewMessage message) {
        message.setSession(this);
        this.messages.add(message);
    }
}
